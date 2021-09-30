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
package Weather;

import hla.rti1516e.*;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.time.HLAfloat64Time;
import org.portico.impl.hla1516e.types.encoding.HLA1516eInteger32BE;

/**
 * This class handles all incoming callbacks from the RTI regarding a particular
 * {@link WeatherFederate}. It will log information about any callbacks it
 * receives, thus demonstrating how to deal with the provided callback information.
 */
public class WeatherFederateAmbassador extends NullFederateAmbassador
{
	private WeatherFederate federate;

	protected double federateTime        = 0.0;
	protected double federateLookahead   = 1.0;
	
	protected boolean isRegulating       = false;
	protected boolean isConstrained      = false;
	protected boolean isAdvancing        = false;
	
	protected boolean isAnnounced        = false;
	protected boolean isReadyToRun       = false;

	protected boolean isRunning       = true;

	public WeatherFederateAmbassador(WeatherFederate federate )
	{
		this.federate = federate;
	}

	private void log( String message )
	{
		System.out.println( "FederateAmbassador: " + message );
	}

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
		if( label.equals(WeatherFederate.READY_TO_RUN) )
			this.isAnnounced = true;
	}

	@Override
	public void federationSynchronized( String label, FederateHandleSet failed )
	{
		log( "Federation Synchronized: " + label );
		if( label.equals(WeatherFederate.READY_TO_RUN) )
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
		builder.append( " handle=" + theObject );
		builder.append( ", tag=" + new String(tag) );
		builder.append( ", attributeCount=" + theAttributes.size() );
		builder.append( "\n" );

		for( AttributeHandle attributeHandle : theAttributes.keySet() )
		{
			builder.append( "\tattributeHandle=" );
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
}
