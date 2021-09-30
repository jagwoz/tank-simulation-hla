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
package GUI;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
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

public class GUIFederate
{
	public static final String READY_TO_RUN = "ReadyToRun";

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private RTIambassador rtiAmb;
	private GUIFederateAmbassador fedAmb;
	private HLAfloat64TimeFactory timeFactory;
	protected EncoderFactory encoderFactory;

	/*x* GuiFederate *x*/
	protected ObjectClassHandle GUIHandle;
	protected AttributeHandle GUIGuiHandle;

	/*x* TankFederate *x*/
	protected ObjectClassHandle tankHandle;
	protected AttributeHandle tankAngleHandle;
	protected AttributeHandle tankVInitHandle;

	/*x* BulletFederate *x*/
	protected ObjectClassHandle bulletHandle;
	protected AttributeHandle bulletSizeHandle;

	/*x* TargetFederate *x*/
	protected ObjectClassHandle targetHandle;
	protected AttributeHandle targetPositionHandle;
	protected AttributeHandle targetSizeHandle;

	/*x* StatisticFederate *x*/
	protected ObjectClassHandle statisticHandle;
	protected AttributeHandle statisticShotsHandle;
	protected AttributeHandle statisticHitsHandle;
	protected AttributeHandle statisticAvgHandle;

	/*x* WeatherFederate *x*/
	protected ObjectClassHandle weatherHandle;
	protected AttributeHandle weatherTemperatureHandle;
	protected AttributeHandle weatherWindHandle;
	protected ObjectClassHandle terrainHandle;
	protected AttributeHandle terrainTerrainHandle;

	/*x* RemoveBullet interaction *x*/
	protected InteractionClassHandle removeBulletHandle;
	protected ParameterHandle wasHitRemoveBulletHandle;
	protected ParameterHandle positionRemoveBulletHandle;

	protected ArrayList<Integer> terrain;
	protected float avg;
	protected int wind;
	protected int temperature;
	protected int targetPosition;
	protected int targetSize;
	protected int shots;
	protected int hits;
	protected int angle;
	protected int viewfinder;
	protected int bulletPositionLast;
	protected int bulletSize;

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	private void log( String message )
	{
		System.out.println( "GUIFederate   : " + message );
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
		GUI.getInstance().addPanel();

		log( "Creating RTIambassador" );
		rtiAmb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
		encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

		log( "Connecting..." );
		fedAmb = new GUIFederateAmbassador( this );
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
		                                "gui",
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

		ObjectInstanceHandle objectHandle = rtiAmb.registerObjectInstance(GUIHandle);
		log( "Registered Terrain, handle=" + objectHandle );

		while( fedAmb.isRunning )
		{
			advanceTime(1);
			log( "Time Advanced to " + fedAmb.federateTime +
					", temperature - " + temperature +
					", wind - " + wind +
					", viewfinder - " + viewfinder +
					", target_position - " + targetPosition +
					", terrain - " + terrain +
					", shots - " + shots +
					", hits - " + hits +
					", avg - " + avg );
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
		/*x* GUI attributes publish *x*/
		this.GUIHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Gui" );
		this.GUIGuiHandle = rtiAmb.getAttributeHandle(GUIHandle, "gui" );
		AttributeHandleSet attributes = rtiAmb.getAttributeHandleSetFactory().create();
		attributes.add(GUIGuiHandle);
		rtiAmb.publishObjectClassAttributes(GUIHandle, attributes);

		/*x* Tank attributes subscribe *x*/
		attributes.clear();
		this.tankHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Tank" );
		this.tankAngleHandle = rtiAmb.getAttributeHandle(tankHandle, "angle" );
		this.tankVInitHandle = rtiAmb.getAttributeHandle(tankHandle, "viewfinder" );
		attributes.add(tankAngleHandle);
		attributes.add(tankVInitHandle);
		rtiAmb.subscribeObjectClassAttributes(tankHandle, attributes);

		/*x* Bullet attributes subscribe *x*/
		attributes.clear();
		this.bulletHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Bullet" );
		this.bulletSizeHandle = rtiAmb.getAttributeHandle(bulletHandle, "size" );
		attributes.add(bulletSizeHandle);
		rtiAmb.subscribeObjectClassAttributes(bulletHandle, attributes );

		/*x* Statistic attributes subscribe *x*/
		attributes.clear();
		this.statisticHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Statistic" );
		this.statisticShotsHandle = rtiAmb.getAttributeHandle(statisticHandle, "shots" );
		this.statisticHitsHandle = rtiAmb.getAttributeHandle(statisticHandle, "hits" );
		this.statisticAvgHandle = rtiAmb.getAttributeHandle(statisticHandle, "avg" );
		attributes.add(statisticShotsHandle);
		attributes.add(statisticHitsHandle);
		attributes.add(statisticAvgHandle);
		rtiAmb.subscribeObjectClassAttributes(statisticHandle, attributes );

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
		this.targetSizeHandle = rtiAmb.getAttributeHandle(targetHandle, "size" );
		attributes.add(targetPositionHandle);
		attributes.add(targetSizeHandle);
		rtiAmb.subscribeObjectClassAttributes(targetHandle, attributes);

		/*x* Terrain attributes subscribe *x*/
		attributes.clear();
		this.terrainHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Terrain" );
		this.terrainTerrainHandle = rtiAmb.getAttributeHandle(terrainHandle, "terrain" );
		attributes.add(terrainTerrainHandle);
		rtiAmb.subscribeObjectClassAttributes(terrainHandle, attributes );

		/*x* RemoveBullet interaction subscribe *x*/
		String iname = "HLAinteractionRoot.RemoveBullet";
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
		String federateName = "GUI";
		if( args.length != 0 )
		{
			federateName = args[0];
		}
		try
		{
			new GUIFederate().runFederate( federateName );
		}
		catch( Exception rtie )
		{
			rtie.printStackTrace();
		}
	}
}