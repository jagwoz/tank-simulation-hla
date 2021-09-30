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
package Tank;

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

public class TankFederate
{
	public static final String READY_TO_RUN = "ReadyToRun";

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private RTIambassador rtiAmb;
	private TankFederateAmbassador fedAmb;
	private HLAfloat64TimeFactory timeFactory;
	protected EncoderFactory encoderFactory;

	/*x* TankFederate *x*/
	protected ObjectClassHandle tankHandle;
	protected AttributeHandle tankAngleHandle;
	protected AttributeHandle tankViewFinderHandle;

	/*x* TargetFederate *x*/
	protected ObjectClassHandle targetHandle;
	protected AttributeHandle targetPositionHandle;
	protected AttributeHandle targetDirectionHandle;

	/*x* Shot Interaction *x*/
	protected InteractionClassHandle shotHandle;
	protected ParameterHandle angleShotHandle;
	protected ParameterHandle vInitShotHandle;

	/*x* RemoveBullet Interaction *x*/
	protected InteractionClassHandle removeBulletHandle;
	protected ParameterHandle wasHitRemoveBulletHandle;
	protected ParameterHandle positionRemoveBulletHandle;

	protected int wasHitLast = 2;
	protected int bulletPositionLast = 0;
	protected int targetPosition = 0;
	protected int targetDirection = 1;

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	private void log( String message )
	{
		System.out.println( "TankFederate:    " + message );
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
		fedAmb = new TankFederateAmbassador( this );
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
		                                "tank",
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

		ObjectInstanceHandle objectHandle = rtiAmb.registerObjectInstance(tankHandle);
		log( "Registered Terrain, handle=" + objectHandle );

		while( fedAmb.isRunning )
		{
			AttributeHandleValueMap attributes = rtiAmb.getAttributeHandleValueMapFactory().create(2);
			HLAinteger32BE angleValue = encoderFactory.createHLAinteger32BE( Tank.getInstance().getAngle());
			attributes.put(tankAngleHandle, angleValue.toByteArray() );
			HLAinteger32BE vinitValue = encoderFactory.createHLAinteger32BE( Tank.getInstance().getViewFinder());
			attributes.put(tankViewFinderHandle, vinitValue.toByteArray() );
			HLAfloat64Time time = timeFactory.makeTime( fedAmb.federateTime+ fedAmb.federateLookahead );
			rtiAmb.updateAttributeValues( objectHandle, attributes, generateTag(), time );

			if(Tank.getInstance().canTankShot()) {
				ParameterHandleValueMap parameterHandleValueMap = rtiAmb.getParameterHandleValueMapFactory().create(2);
				ParameterHandle shootAngleHandle = rtiAmb.getParameterHandle(shotHandle, "viewfinder");
				HLAinteger32BE angle = encoderFactory.createHLAinteger32BE(targetDirection);
				parameterHandleValueMap.put(shootAngleHandle, angle.toByteArray());
				ParameterHandle shootVInitHandle = rtiAmb.getParameterHandle(shotHandle, "target_direction");
				HLAinteger32BE vInit = encoderFactory.createHLAinteger32BE(Tank.getInstance().getViewFinder());
				parameterHandleValueMap.put(shootVInitHandle, vInit.toByteArray());

				rtiAmb.sendInteraction(shotHandle, parameterHandleValueMap, generateTag(), time);
				log("Interaction Shot send whit parameters (viewfinder: " + Tank.getInstance().getViewFinder() + ", target_direction: " + targetDirection);
			}

			advanceTime(1);
			log( "Time Advanced to " + fedAmb.federateTime +
					", angle - " + Tank.getInstance().getAngle() +
					", view_finder - " + Tank.getInstance().getViewFinder());
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
	
	public void calculateTarget(int pos){
		Tank.getInstance().calculateAngle(pos);
	}

	public void newShoot(int targetPos, int targetDirect, int bulletPos, int wasHitLastBullet){
		Tank.getInstance().calculateMoreShoots(targetPos, targetDirect, bulletPos, wasHitLastBullet);
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
		/*x* Tank attributes publish *x*/
		this.tankHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Tank" );
		this.tankAngleHandle = rtiAmb.getAttributeHandle(tankHandle, "angle" );
		this.tankViewFinderHandle = rtiAmb.getAttributeHandle(tankHandle, "viewfinder" );
		AttributeHandleSet attributes = rtiAmb.getAttributeHandleSetFactory().create();
		attributes.add(tankAngleHandle);
		attributes.add(tankViewFinderHandle);
		rtiAmb.publishObjectClassAttributes(tankHandle, attributes );

		/*x* Target attributes subscribe *x*/
		attributes.clear();
		this.targetHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Target" );
		this.targetPositionHandle = rtiAmb.getAttributeHandle(targetHandle, "position" );
		this.targetDirectionHandle = rtiAmb.getAttributeHandle(targetHandle, "direction" );
		attributes.add(targetPositionHandle);
		attributes.add(targetDirectionHandle);
		rtiAmb.subscribeObjectClassAttributes(targetHandle, attributes);

		/*x* Shot interaction publish *x*/
		String iname = "HLAinteractionRoot.Shot";
		shotHandle = rtiAmb.getInteractionClassHandle( iname );
		angleShotHandle = rtiAmb.getParameterHandle(rtiAmb.getInteractionClassHandle( "HLAinteractionRoot.Shot" ), "viewfinder");
		vInitShotHandle = rtiAmb.getParameterHandle(rtiAmb.getInteractionClassHandle( "HLAinteractionRoot.Shot" ), "target_direction");
		rtiAmb.publishInteractionClass(shotHandle);

		/*x* RemoveBullet interaction subscribe *x*/
		iname = "HLAinteractionRoot.RemoveBullet";
		removeBulletHandle = rtiAmb.getInteractionClassHandle( iname );
		wasHitRemoveBulletHandle = rtiAmb.getParameterHandle(rtiAmb.getInteractionClassHandle( "HLAinteractionRoot.RemoveBullet" ), "was_hit");
		positionRemoveBulletHandle = rtiAmb.getParameterHandle(rtiAmb.getInteractionClassHandle( "HLAinteractionRoot.RemoveBullet" ), "bullet_position");
		rtiAmb.subscribeInteractionClass(removeBulletHandle);
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
		String federateName = "Tank";
		if( args.length != 0 )
		{
			federateName = args[0];
		}
		try
		{
			new TankFederate().runFederate( federateName );
		}
		catch( Exception rtie )
		{
			rtie.printStackTrace();
		}
	}
}