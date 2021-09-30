/*
 *   Copyright 2012 The Portico Project
 *
 *   This file is part of portico.
 *
 *   portico is free software; you can redistribute it and/or modify
 *   it under the terms of the Common Developer and Distribution License (CDDL) 
 *   as published by Sun Microsystems. For more information see the LICENSE file.
 *   
 *   Use of this software is strictly AT YOUR OWN RISK!!!
 *   If something bad happens you do not have permission to come crying to me.
 *   (that goes for your lawyer as well)
 *
 */
package Target;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class TargetFederate
{
	public static final String READY_TO_RUN = "ReadyToRun";

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private RTIambassador rtiAmb;
	private TargetFederateAmbassador fedAmb;
	private HLAfloat64TimeFactory timeFactory;
	protected EncoderFactory encoderFactory;

	/*x* TargetFederate *x*/
	protected ObjectClassHandle targetHandle;
	protected AttributeHandle targetPositionHandle;
	protected AttributeHandle targetSizeHandle;
	protected AttributeHandle targetDirectionHandle;

	/*x* TerrainFederate *x*/
	protected InteractionClassHandle terrainUploadHandle;
	protected ObjectClassHandle terrainHandle;
	protected AttributeHandle terrainTerrainHandle;

	/*x* WeatherFederate *x*/
	protected ObjectClassHandle weatherHandle;
	protected AttributeHandle weatherTemperatureHandle;

	protected int temperature = 0;
	protected ArrayList<Integer> terrain;

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	private void log( String message )
	{
		System.out.println( "TargetFederate:    " + message );
	}

	private void waitForUser()
	{
		log( " >>>>>>>>>> Press Enter to Continue <<<<<<<<<<" );
		BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
		try
		{
			reader.readLine();
		}
		catch( Exception e )
		{
			log( "Error while waiting for user input: " + e.getMessage() );
			e.printStackTrace();
		}
	}

	//----------------------------------------------------------
	//                 MAIN SIMULATION METHODS
	//----------------------------------------------------------
	public void runFederate( String federateName ) throws Exception
	{
		log( "Creating RTIambassador" );
		rtiAmb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
		encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

		log( "Connecting..." );
		fedAmb = new TargetFederateAmbassador( this );
		rtiAmb.connect(fedAmb, CallbackModel.HLA_EVOKED );

		log( "Creating Federation..." );
		try
		{
			URL[] modules = new URL[]{
			    (new File("foms/TankSimulator.xml")).toURI().toURL(),
			};
			
			rtiAmb.createFederationExecution( "TankSimulationFederation", modules );
			log( "Created Federation" );
		}
		catch( FederationExecutionAlreadyExists exists )
		{
			log( "Didn't create federation, it already existed" );
		}
		catch( MalformedURLException urle )
		{
			log( "Exception loading one of the FOM modules from disk: " + urle.getMessage() );
			urle.printStackTrace();
			return;
		}

		rtiAmb.joinFederationExecution( federateName,
		                                "target",
		                                "TankSimulationFederation"
		                                 );

		log( "Joined Federation as " + federateName );

		this.timeFactory = (HLAfloat64TimeFactory) rtiAmb.getTimeFactory();

		rtiAmb.registerFederationSynchronizationPoint( READY_TO_RUN, null );
		while( fedAmb.isAnnounced == false )
		{
			rtiAmb.evokeMultipleCallbacks( 0.1, 0.2 );
		}

		waitForUser();

		rtiAmb.synchronizationPointAchieved( READY_TO_RUN );
		log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
		while( fedAmb.isReadyToRun == false )
		{
			rtiAmb.evokeMultipleCallbacks( 0.1, 0.2 );
		}

		enableTimePolicy();
		log( "Time Policy Enabled" );

		publishAndSubscribe();
		log( "Published and Subscribed" );

		ObjectInstanceHandle objectHandle = rtiAmb.registerObjectInstance(targetHandle);
		log( "Registered Terrain, handle=" + objectHandle );

		while( fedAmb.isRunning )
		{
			AttributeHandleValueMap attributes = rtiAmb.getAttributeHandleValueMapFactory().create(3);
			HLAinteger32BE positionValue = encoderFactory.createHLAinteger32BE( Target.getInstance().getPosition());
			attributes.put(targetPositionHandle, positionValue.toByteArray() );
			HLAinteger32BE sizeValue = encoderFactory.createHLAinteger32BE( Target.getInstance().getSize() );
			attributes.put(targetSizeHandle, sizeValue.toByteArray() );
			HLAinteger32BE directionValue = encoderFactory.createHLAinteger32BE( Target.getInstance().getDirection() );
			attributes.put(targetDirectionHandle, directionValue.toByteArray() );
			HLAfloat64Time time = timeFactory.makeTime( fedAmb.federateTime+ fedAmb.federateLookahead );

			rtiAmb.updateAttributeValues( objectHandle, attributes, generateTag(), time );

			Target.getInstance().updatePosition(this.temperature);

			if(Target.getInstance().isNeedTerrainUpdate()) {
				ParameterHandleValueMap parameterHandleValueMap = rtiAmb.getParameterHandleValueMapFactory().create(0);
				rtiAmb.sendInteraction(terrainUploadHandle, parameterHandleValueMap, generateTag(), time);
				log("Interaction TerrainUpload send.");
			}

			advanceTime(1);
			log( "Time Advanced to " + fedAmb.federateTime +
					", position - " + Target.getInstance().getPosition() +
					", size - " + Target.getInstance().getSize() +
					", direction - " + Target.getInstance().getDirection() +
					", temperature - " + temperature +
					", terrain - " + terrain);
		}

		rtiAmb.resignFederationExecution( ResignAction.DELETE_OBJECTS );
		log( "Resigned from Federation" );

		try
		{
			rtiAmb.destroyFederationExecution( "ExampleFederation" );
			log( "Destroyed Federation" );
		}
		catch( FederationExecutionDoesNotExist dne )
		{
			log( "No need to destroy federation, it doesn't exist" );
		}
		catch( FederatesCurrentlyJoined fcj )
		{
			log( "Didn't destroy federation, federates still joined" );
		}
	}

	private void enableTimePolicy() throws Exception
	{
		HLAfloat64Interval lookahead = timeFactory.makeInterval( fedAmb.federateLookahead );

		this.rtiAmb.enableTimeRegulation( lookahead );
		while( fedAmb.isRegulating == false )
		{
			rtiAmb.evokeMultipleCallbacks( 0.1, 0.2 );
		}

		this.rtiAmb.enableTimeConstrained();
		while( fedAmb.isConstrained == false )
		{
			rtiAmb.evokeMultipleCallbacks( 0.1, 0.2 );
		}
	}

	private void publishAndSubscribe() throws RTIexception
	{
		/*x* Target attributes publish *x*/
		this.targetHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Target" );
		this.targetPositionHandle = rtiAmb.getAttributeHandle(targetHandle, "position" );
		this.targetSizeHandle = rtiAmb.getAttributeHandle(targetHandle, "size" );
		this.targetDirectionHandle = rtiAmb.getAttributeHandle(targetHandle, "direction" );
		AttributeHandleSet attributes = rtiAmb.getAttributeHandleSetFactory().create();
		attributes.add(targetPositionHandle);
		attributes.add(targetSizeHandle);
		attributes.add(targetDirectionHandle);
		rtiAmb.publishObjectClassAttributes(targetHandle, attributes );

		/*x* Weather attributes subscribe *x*/
		attributes.clear();
		this.weatherHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Weather" );
		this.weatherTemperatureHandle = rtiAmb.getAttributeHandle(weatherHandle, "temperature" );
		attributes.add(weatherTemperatureHandle);
		rtiAmb.subscribeObjectClassAttributes(weatherHandle, attributes );

		/*x* Terrain attributes subscribe *x*/
		attributes.clear();
		this.terrainHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Terrain" );
		this.terrainTerrainHandle = rtiAmb.getAttributeHandle(terrainHandle, "terrain" );
		attributes.add(terrainTerrainHandle);
		rtiAmb.subscribeObjectClassAttributes(terrainHandle, attributes );

		/*x* TerrainUpload interaction publish *x*/
		String iname = "HLAinteractionRoot.TerrainUpload";
		terrainUploadHandle = rtiAmb.getInteractionClassHandle( iname );
		rtiAmb.publishInteractionClass(terrainUploadHandle);
	}

	private void advanceTime( double timestep ) throws RTIexception
	{
		fedAmb.isAdvancing = true;
		HLAfloat64Time time = timeFactory.makeTime( fedAmb.federateTime + timestep );
		rtiAmb.timeAdvanceRequest( time );

		while( fedAmb.isAdvancing )
		{
			rtiAmb.evokeMultipleCallbacks( 0.1, 0.2 );
		}
	}

	private short getTimeAsShort()
	{
		return (short) fedAmb.federateTime;
	}
	private byte[] generateTag()
	{
		return ("(timestamp) "+System.currentTimeMillis()).getBytes();
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
	public static void main( String[] args )
	{
		String federateName = "Target";
		if( args.length != 0 )
		{
			federateName = args[0];
		}
		try
		{
			new TargetFederate().runFederate( federateName );
		}
		catch( Exception rtie )
		{
			rtie.printStackTrace();
		}
	}
}