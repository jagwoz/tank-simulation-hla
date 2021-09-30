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
package Bullet;

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

public class BulletFederate
{
	public static final String READY_TO_RUN = "ReadyToRun";

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private RTIambassador rtiAmb;
	private BulletFederateAmbassador fedAmb;
	private HLAfloat64TimeFactory timeFactory;
	protected EncoderFactory encoderFactory;

	/*x* BulletFederate *x*/
	protected ObjectClassHandle bulletHandle;
	protected AttributeHandle bulletSizeHandle;
	/*x* WeatherFederate *x*/
	protected ObjectClassHandle weatherHandle;
	protected AttributeHandle weatherTemperatureHandle;
	protected AttributeHandle weatherWindHandle;
	/*x* TargetFederate *x*/
	protected ObjectClassHandle targetHandle;
	protected AttributeHandle targetSizeHandle;
	protected AttributeHandle targetPositionHandle;
	protected AttributeHandle targetDirectionHandle;
	/*x* TerrainFederate *x*/
	protected ObjectClassHandle terrainHandle;
	protected AttributeHandle terrainTerrainHandle;
	/*x* RemoveBullet Interaction *x*/
	protected InteractionClassHandle removeBulletHandle;
	protected ParameterHandle wasHitRemoveBulletHandle;
	protected ParameterHandle bulletPositionRemoveBulletHandle;
	/*x* Shot Interaction *x*/
	protected InteractionClassHandle shotHandle;
	protected ParameterHandle targetDirectionShotHandle;
	protected ParameterHandle viewfinderShotHandle;

	private int mistake = 3;
	protected ArrayList<Integer> terrain;
	protected int temperature;
	protected int wind;
	protected int targetPosition;
	protected int targetSize;
	protected int targetDirection;
	protected int angle;
	protected int viewfinder;

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	private void log( String message )
	{
		System.out.println( "BulletFederate:    " + message );
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
	public void runFederate( String federateName ) throws Exception {
		log( "Creating RTIambassador" );
		rtiAmb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
		encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

		log( "Connecting..." );
		fedAmb = new BulletFederateAmbassador( this );
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
		                                "bullet",
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
		log( "Achieved sync point: " + READY_TO_RUN + ", waiting for federation..." );
		while( fedAmb.isReadyToRun == false )
		{
			rtiAmb.evokeMultipleCallbacks( 0.1, 0.2 );
		}

		enableTimePolicy();
		log( "Time Policy Enabled" );

		publishAndSubscribe();
		log( "Published and Subscribed" );

		ObjectInstanceHandle objectHandle = rtiAmb.registerObjectInstance(bulletHandle);
		log( "Registered Terrain, handle=" + objectHandle );

		while( fedAmb.isRunning )
		{
			AttributeHandleValueMap attributes = rtiAmb.getAttributeHandleValueMapFactory().create(1);
			HLAinteger32BE sizeValue = encoderFactory.createHLAinteger32BE( Bullet.getInstance().getSize() );
			attributes.put(bulletSizeHandle, sizeValue.toByteArray() );
			HLAfloat64Time time = timeFactory.makeTime( fedAmb.federateTime+ fedAmb.federateLookahead );
			rtiAmb.updateAttributeValues( objectHandle, attributes, generateTag(), time );

			Bullet.getInstance().updatePosition(this.temperature, this.wind);
			if(Bullet.getInstance().isRemoveBulletNeed()) {
				int bulletPosition;
				if(angle == 1){
					if(viewfinder + Bullet.getInstance().getPositionParam() <= 99)
						bulletPosition = viewfinder + Bullet.getInstance().getPositionParam();
					else bulletPosition = 99 - (viewfinder + Bullet.getInstance().getPositionParam() - 99);
				} else {
					if(viewfinder - Bullet.getInstance().getPositionParam() >= 0)
						bulletPosition = viewfinder - Bullet.getInstance().getPositionParam();
					else bulletPosition = Math.abs(viewfinder - Bullet.getInstance().getPositionParam());
				}

				System.out.println(Bullet.getInstance().getPositionParam());

				String toSend = "";
				{
					if (bulletPosition > targetPosition) toSend += "1";
					else if (bulletPosition < targetPosition) toSend += "2";
					else toSend += "3";
					if (Math.abs(bulletPosition - targetPosition) <= mistake && terrain.get(bulletPosition) == terrain.get(targetPosition))
						toSend += "1";
					else toSend += "0";
					toSend += String.valueOf(angle);
					toSend += String.valueOf(targetDirection);
				}
				ParameterHandleValueMap parameterHandleValueMap = rtiAmb.getParameterHandleValueMapFactory().create(2);
				ParameterHandle shootAngleHandle = rtiAmb.getParameterHandle(removeBulletHandle, "was_hit");
				HLAinteger32BE angle = encoderFactory.createHLAinteger32BE(Integer.valueOf(toSend));
				parameterHandleValueMap.put(shootAngleHandle, angle.toByteArray());
				ParameterHandle shootVInitHandle = rtiAmb.getParameterHandle(removeBulletHandle, "bullet_position");
				HLAinteger32BE vInit = encoderFactory.createHLAinteger32BE(bulletPosition);
				parameterHandleValueMap.put(shootVInitHandle, vInit.toByteArray());
				rtiAmb.sendInteraction(removeBulletHandle, parameterHandleValueMap, generateTag(), time);
				log("Interaction BulletRemove send with params (was_hit=" + Integer.valueOf(toSend) + ", bullet_position=" + bulletPosition + ")");
			}

			advanceTime(1);
			log( "Time Advanced to " + fedAmb.federateTime +
					", size - " + Bullet.getInstance().getSize() +
					", target (size, position) - " + targetSize + ", " + targetPosition +
					", target_direction_last - " + angle +
					", viewfinder_last - " + viewfinder +
				    ", weather (temperature, wind)  - " + temperature + " " + wind +
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

	public void launchNewBullet(){
		Bullet.getInstance().launchBullet();
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
		/*x* Bullet attributes publish *x*/
		this.bulletHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Bullet" );
		this.bulletSizeHandle = rtiAmb.getAttributeHandle(bulletHandle, "size" );
		AttributeHandleSet attributes = rtiAmb.getAttributeHandleSetFactory().create();
		attributes.add(bulletSizeHandle);
		rtiAmb.publishObjectClassAttributes(bulletHandle, attributes );

		/*x* Weather attributes subscribe *x*/
		attributes.clear();
		this.weatherHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Weather" );
		this.weatherTemperatureHandle = rtiAmb.getAttributeHandle(weatherHandle, "temperature" );
		this.weatherWindHandle = rtiAmb.getAttributeHandle(weatherHandle, "wind" );
		attributes.add(weatherTemperatureHandle);
		attributes.add(weatherWindHandle);
		rtiAmb.subscribeObjectClassAttributes(weatherHandle, attributes );

		/*x* Target attributes subscribe *x*/
		attributes.clear();
		this.targetHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Target" );
		this.targetPositionHandle = rtiAmb.getAttributeHandle(targetHandle, "position" );
		this.targetDirectionHandle = rtiAmb.getAttributeHandle(targetHandle, "direction" );
		this.targetSizeHandle = rtiAmb.getAttributeHandle(targetHandle, "size" );
		attributes.add(targetPositionHandle);
		attributes.add(targetDirectionHandle);
		attributes.add(targetSizeHandle);
		rtiAmb.subscribeObjectClassAttributes(targetHandle, attributes);

		/*x* Terrain attributes subscribe *x*/
		attributes.clear();
		this.terrainHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Terrain" );
		this.terrainTerrainHandle = rtiAmb.getAttributeHandle(terrainHandle, "terrain" );
		attributes.add(terrainTerrainHandle);
		rtiAmb.subscribeObjectClassAttributes(terrainHandle, attributes );

		/*x* RemoveBullet interaction publish *x*/
		String iname = "HLAinteractionRoot.RemoveBullet";
		removeBulletHandle = rtiAmb.getInteractionClassHandle( iname );
		wasHitRemoveBulletHandle = rtiAmb.getParameterHandle(rtiAmb.getInteractionClassHandle( "HLAinteractionRoot.RemoveBullet" ), "was_hit");
		bulletPositionRemoveBulletHandle = rtiAmb.getParameterHandle(rtiAmb.getInteractionClassHandle( "HLAinteractionRoot.RemoveBullet" ), "bullet_position");
		rtiAmb.publishInteractionClass(removeBulletHandle);

		/*x* Shot interaction subscribe *x*/
		iname = "HLAinteractionRoot.Shot";
		shotHandle = rtiAmb.getInteractionClassHandle( iname );
		targetDirectionShotHandle = rtiAmb.getParameterHandle(rtiAmb.getInteractionClassHandle( "HLAinteractionRoot.Shot" ), "viewfinder");
		viewfinderShotHandle = rtiAmb.getParameterHandle(rtiAmb.getInteractionClassHandle( "HLAinteractionRoot.Shot" ), "target_direction");
		rtiAmb.subscribeInteractionClass(shotHandle);
	}

	private void advanceTime( double timeStep ) throws RTIexception
	{
		fedAmb.isAdvancing = true;
		HLAfloat64Time time = timeFactory.makeTime( fedAmb.federateTime + timeStep );
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
		return ("(timestamp) " + System.currentTimeMillis()).getBytes();
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
	public static void main( String[] args )
	{
		String federateName = "Bullet";
		if( args.length != 0 )
		{
			federateName = args[0];
		}
		
		try
		{
			new BulletFederate().runFederate( federateName );
		}
		catch( Exception rti )
		{
			rti.printStackTrace();
		}
	}
}