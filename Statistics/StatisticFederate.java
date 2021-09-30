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
package Statistics;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAfloat32BE;
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

public class StatisticFederate
{
	public static final String READY_TO_RUN = "ReadyToRun";

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private RTIambassador rtiAmb;
	private StatisticFederateAmbassador fedAmb;
	private HLAfloat64TimeFactory timeFactory;
	protected EncoderFactory encoderFactory;

	/*x* StatisticFederate *x*/
	protected ObjectClassHandle statisticHandle;
	protected AttributeHandle statisticShotsHandle;
	protected AttributeHandle statisticHitsHandle;
	protected AttributeHandle statisticAvgHandle;

	/*x* Shot Interaction *x*/
	protected InteractionClassHandle shotHandle;
	protected ParameterHandle targetDirectionShotHandle;
	protected ParameterHandle viewfinderShotHandle;

	/*x* RemoveBullet Interaction *x*/
	protected InteractionClassHandle removeBulletHandle;
	protected ParameterHandle wasHitRemoveBulletHandle;
	protected ParameterHandle positionRemoveBulletHandle;

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	private void log( String message )
	{
		System.out.println( "StatisticFederate:    " + message );
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
		fedAmb = new StatisticFederateAmbassador( this );
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
		                                "statistic",
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

		ObjectInstanceHandle objectHandle = rtiAmb.registerObjectInstance(statisticHandle);
		log( "Registered Terrain, handle=" + objectHandle );

		while( fedAmb.isRunning )
		{
			Statistic.getInstance().updateAvg(fedAmb.federateTime);

			AttributeHandleValueMap attributes = rtiAmb.getAttributeHandleValueMapFactory().create(3);
			HLAinteger32BE shotsValue = encoderFactory.createHLAinteger32BE( Statistic.getInstance().getShots());
			attributes.put(statisticShotsHandle, shotsValue.toByteArray() );
			HLAinteger32BE hitsValue = encoderFactory.createHLAinteger32BE( Statistic.getInstance().getHits());
			attributes.put(statisticHitsHandle, hitsValue.toByteArray() );
			HLAfloat32BE avgValue = encoderFactory.createHLAfloat32BE( Statistic.getInstance().getAvgChance());
			attributes.put(statisticAvgHandle, avgValue.toByteArray() );
			HLAfloat64Time time = timeFactory.makeTime( fedAmb.federateTime+ fedAmb.federateLookahead );
			rtiAmb.updateAttributeValues( objectHandle, attributes, generateTag(), time );

			advanceTime(1);
			log( "Time Advanced to " + fedAmb.federateTime +
					", shots - " + Statistic.getInstance().getShots() +
					", hits - " + Statistic.getInstance().getHits() +
					", avg - " + Statistic.getInstance().getAvgChance());
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
	
	public void addShot(){
		Statistic.getInstance().addShot();
	}

	public void addHit(int wasHit){
		Statistic.getInstance().addHit(wasHit, (double) fedAmb.federateTime);
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
		/*x* Statistic attributes publish *x*/
		this.statisticHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Statistic" );
		this.statisticShotsHandle = rtiAmb.getAttributeHandle(statisticHandle, "shots" );
		this.statisticHitsHandle = rtiAmb.getAttributeHandle(statisticHandle, "hits" );
		this.statisticAvgHandle = rtiAmb.getAttributeHandle(statisticHandle, "avg" );
		AttributeHandleSet attributes = rtiAmb.getAttributeHandleSetFactory().create();
		attributes.add(statisticShotsHandle);
		attributes.add(statisticHitsHandle);
		attributes.add(statisticAvgHandle);
		rtiAmb.publishObjectClassAttributes(statisticHandle, attributes );

		/*x* Shot interaction subscribe *x*/
		String iname = "HLAinteractionRoot.Shot";
		shotHandle = rtiAmb.getInteractionClassHandle( iname );
		targetDirectionShotHandle = rtiAmb.getParameterHandle(rtiAmb.getInteractionClassHandle( "HLAinteractionRoot.Shot" ), "viewfinder");
		viewfinderShotHandle = rtiAmb.getParameterHandle(rtiAmb.getInteractionClassHandle( "HLAinteractionRoot.Shot" ), "target_direction");
		rtiAmb.subscribeInteractionClass(shotHandle);

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
		String federateName = "Statistic";
		if( args.length != 0 )
		{
			federateName = args[0];
		}
		
		try
		{
			new StatisticFederate().runFederate( federateName );
		}
		catch( Exception rti )
		{
			rti.printStackTrace();
		}
	}
}