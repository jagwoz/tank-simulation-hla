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
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.time.HLAfloat64Time;
import org.portico.impl.hla1516e.types.encoding.HLA1516eInteger32BE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

/**
 * This class handles all incoming callbacks from the RTI regarding a particular
 * {@link BulletFederate}. It will log information about any callbacks it
 * receives, thus demonstrating how to deal with the provided callback information.
 */
public class BulletFederateAmbassador extends NullFederateAmbassador
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private BulletFederate federate;

	// these variables are accessible in the package
	protected double federateTime        = 0.0;
	protected double federateLookahead   = 1.0;

	protected boolean isRegulating       = false;
	protected boolean isConstrained      = false;
	protected boolean isAdvancing        = false;

	protected boolean isAnnounced        = false;
	protected boolean isReadyToRun       = false;

	protected boolean isRunning       = true;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	public BulletFederateAmbassador(BulletFederate federate )
	{
		this.federate = federate;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	private void log( String message )
	{
		System.out.println( "FederateAmbassador: " + message );
	}

	//////////////////////////////////////////////////////////////////////////
	////////////////////////// RTI Callback Methods //////////////////////////
	//////////////////////////////////////////////////////////////////////////
	@Override
	public void synchronizationPointRegistrationFailed( String label,
	                                                    SynchronizationPointFailureReason reason )
	{
		log( "Failed to register sync point: " + label + ", reason="+reason );
	}

	@Override
	public void synchronizationPointRegistrationSucceeded( String label )
	{
		log( "Successfully registered sync point: " + label );
	}

	@Override
	public void announceSynchronizationPoint( String label, byte[] tag )
	{
		log( "Synchronization point announced: " + label );
		if( label.equals(BulletFederate.READY_TO_RUN) )
			this.isAnnounced = true;
	}

	@Override
	public void federationSynchronized( String label, FederateHandleSet failed )
	{
		log( "Federation Synchronized: " + label );
		if( label.equals(BulletFederate.READY_TO_RUN) )
			this.isReadyToRun = true;
	}

	/**
	 * The RTI has informed us that time regulation is now enabled.
	 */
	@Override
	public void timeRegulationEnabled( LogicalTime time )
	{
		this.federateTime = ((HLAfloat64Time)time).getValue();
		this.isRegulating = true;
	}

	@Override
	public void timeConstrainedEnabled( LogicalTime time )
	{
		this.federateTime = ((HLAfloat64Time)time).getValue();
		this.isConstrained = true;
	}

	@Override
	public void timeAdvanceGrant( LogicalTime time )
	{
		this.federateTime = ((HLAfloat64Time)time).getValue();
		this.isAdvancing = false;
	}

	@Override
	public void discoverObjectInstance( ObjectInstanceHandle theObject,
	                                    ObjectClassHandle theObjectClass,
	                                    String objectName )
	    throws FederateInternalError
	{
		log( "Discoverd Object: handle=" + theObject + ", classHandle=" +
		     theObjectClass + ", name=" + objectName );
	}

	@Override
	public void reflectAttributeValues( ObjectInstanceHandle theObject,
	                                    AttributeHandleValueMap theAttributes,
	                                    byte[] tag,
	                                    OrderType sentOrder,
	                                    TransportationTypeHandle transport,
	                                    SupplementalReflectInfo reflectInfo )
	    throws FederateInternalError
	{
			// just pass it on to the other method for printing purposes
			// passing null as the time will let the other method know it
			// it from us, not from the RTI
			reflectAttributeValues( theObject,
			                        theAttributes,
			                        tag,
			                        sentOrder,
			                        transport,
			                        null,
			                        sentOrder,
			                        reflectInfo );
	}

	@Override
	public void reflectAttributeValues( ObjectInstanceHandle theObject,
	                                    AttributeHandleValueMap theAttributes,
	                                    byte[] tag,
	                                    OrderType sentOrdering,
	                                    TransportationTypeHandle theTransport,
	                                    LogicalTime time,
	                                    OrderType receivedOrdering,
	                                    SupplementalReflectInfo reflectInfo )
	    throws FederateInternalError
	{
		StringBuilder builder = new StringBuilder( "Reflection for object:" );

		// print the handle
		builder.append( " handle=" + theObject );
		// print the tag
		builder.append( ", tag=" + new String(tag) );
		// print the time (if we have it) we'll get null if we are just receiving
		// a forwarded call from the other reflect callback above
		if( time != null )
		{
			builder.append( ", time=" + ((HLAfloat64Time)time).getValue() );
		}

		// print the attribute information
		builder.append( ", attributeCount=" + theAttributes.size() );
		builder.append( "\n" );
		for( AttributeHandle attributeHandle : theAttributes.keySet() )
		{
			// print the attibute handle
			builder.append( "\tattributeHandle=" );

			// if we're dealing with Flavor, decode into the appropriate enum value
			if( attributeHandle.equals(federate.terrainTerrainHandle) )
			{
				builder.append( attributeHandle );
				builder.append( " (Terrain)    " );
				builder.append( ", attributeValue=" );
				ByteArrayInputStream bis = new ByteArrayInputStream(theAttributes.get(federate.terrainTerrainHandle));
				try {
					ObjectInputStream ois = new ObjectInputStream(bis);
					ArrayList<Integer> decode = (ArrayList<Integer>) ois.readObject();
					federate.terrain = decode;
					builder.append(decode);
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			else if( attributeHandle.equals(federate.weatherWindHandle) )
			{
				builder.append( attributeHandle );
				builder.append( " (Wind)    " );
				builder.append( ", attributeValue=" );
				HLAinteger32BE wind = new HLA1516eInteger32BE();
				try {
					wind.decode(theAttributes.get(attributeHandle));
				} catch (DecoderException e) {
					e.printStackTrace();
				}
				builder.append( wind.getValue() );
				federate.wind = wind.getValue();
			}
			else if( attributeHandle.equals(federate.targetPositionHandle) )
			{
				builder.append( attributeHandle );
				builder.append( " (Target position)    " );
				builder.append( ", attributeValue=" );
				HLAinteger32BE targetPos = new HLA1516eInteger32BE();
				try {
					targetPos.decode(theAttributes.get(attributeHandle));
				} catch (DecoderException e) {
					e.printStackTrace();
				}
				builder.append( targetPos.getValue() );
				federate.targetPosition = targetPos.getValue();
			}
			else if( attributeHandle.equals(federate.targetSizeHandle) )
			{
				builder.append( attributeHandle );
				builder.append( " (Target size)    " );
				builder.append( ", attributeValue=" );
				HLAinteger32BE targetPos = new HLA1516eInteger32BE();
				try {
					targetPos.decode(theAttributes.get(attributeHandle));
				} catch (DecoderException e) {
					e.printStackTrace();
				}
				builder.append( targetPos.getValue() );
				federate.targetSize = targetPos.getValue();
			}
			else if( attributeHandle.equals(federate.targetDirectionHandle) )
			{
				builder.append( attributeHandle );
				builder.append( " (Target direction)    " );
				builder.append( ", attributeValue=" );
				HLAinteger32BE targetPos = new HLA1516eInteger32BE();
				try {
					targetPos.decode(theAttributes.get(attributeHandle));
				} catch (DecoderException e) {
					e.printStackTrace();
				}
				builder.append( targetPos.getValue() );
				federate.targetDirection = targetPos.getValue();
			}
			else if( attributeHandle.equals(federate.weatherTemperatureHandle) )
			{
				builder.append( attributeHandle );
				builder.append( " (Temperature)    " );
				builder.append( ", attributeValue=" );
				HLAinteger32BE temperature = new HLA1516eInteger32BE();
				try {
					temperature.decode(theAttributes.get(attributeHandle));
				} catch (DecoderException e) {
					e.printStackTrace();
				}
				builder.append( temperature.getValue() );
				federate.temperature = temperature.getValue();
			}
			else
			{
				builder.append( " " + attributeHandle );
				builder.append( " (Unknown)   " );
			}

			builder.append( "\n" );
		}

		log( builder.toString() );
	}

	@Override
	public void receiveInteraction( InteractionClassHandle interactionClass,
	                                ParameterHandleValueMap theParameters,
	                                byte[] tag,
	                                OrderType sentOrdering,
	                                TransportationTypeHandle theTransport,
	                                SupplementalReceiveInfo receiveInfo )
	    throws FederateInternalError
	{
		// just pass it on to the other method for printing purposes
		// passing null as the time will let the other method know it
		// it from us, not from the RTI
		this.receiveInteraction( interactionClass,
		                         theParameters,
		                         tag,
		                         sentOrdering,
		                         theTransport,
		                         null,
		                         sentOrdering,
		                         receiveInfo );
	}

	@Override
	public void receiveInteraction( InteractionClassHandle interactionClass,
	                                ParameterHandleValueMap theParameters,
	                                byte[] tag,
	                                OrderType sentOrdering,
	                                TransportationTypeHandle theTransport,
	                                LogicalTime time,
	                                OrderType receivedOrdering,
	                                SupplementalReceiveInfo receiveInfo )
	    throws FederateInternalError
	{
		StringBuilder builder = new StringBuilder( "Interaction Received:" );

		// print the handle
		builder.append( " handle=" + interactionClass );

		if( interactionClass.equals(federate.shotHandle) )
		{
			builder.append( " (Shot)" );
		}

		// print the tag
		builder.append( ", tag=" + new String(tag) );
		// print the time (if we have it) we'll get null if we are just receiving
		// a forwarded call from the other reflect callback above
		if( time != null )
		{
			builder.append( ", time=" + ((HLAfloat64Time)time).getValue() );
		}

		// print the parameer information
		builder.append( ", parameterCount=" + theParameters.size() );
		builder.append( "\n" );
		for( ParameterHandle parameter : theParameters.keySet() )
		{
			if(parameter.equals(federate.targetDirectionShotHandle))
			{
				builder.append( "\tShot param" );
				byte[] bytes = theParameters.get(federate.targetDirectionShotHandle);
				HLAinteger32BE angle = new HLA1516eInteger32BE();
				try {
					angle.decode(bytes);
				} catch (DecoderException e) {
					e.printStackTrace();
				}
				int countValue = angle.getValue();
				federate.angle = countValue;
				builder.append( "\ttarget_direction=" + countValue );
			}
			else if(parameter.equals(federate.viewfinderShotHandle))
			{
				builder.append( "\tShot param" );
				byte[] bytes = theParameters.get(federate.viewfinderShotHandle);
				HLAinteger32BE vInit = new HLA1516eInteger32BE();
				try {
					vInit.decode(bytes);
				} catch (DecoderException e) {
					e.printStackTrace();
				}
				int countValue = vInit.getValue();
				federate.viewfinder = countValue;
				builder.append( "\tviewfinder=" + countValue );
			}
			else
			{
				// print the parameter handle
				builder.append( "\tparamHandle=" );
				builder.append( parameter );
				// print the parameter value
				builder.append( ", paramValue=" );
				builder.append( theParameters.get(parameter).length );
				builder.append( " bytes" );
				builder.append( "\n" );
			}
		}

		federate.launchNewBullet();

		log( builder.toString() );
	}

	@Override
	public void removeObjectInstance( ObjectInstanceHandle theObject,
	                                  byte[] tag,
	                                  OrderType sentOrdering,
	                                  SupplementalRemoveInfo removeInfo )
	    throws FederateInternalError
	{
		log( "Object Removed: handle=" + theObject );
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
