/*
 * Copyright 2010, OpenPlans Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.webapp.actions.api.siri;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.impl.service_alerts.ServiceAlertsHelper;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;

import org.apache.struts2.interceptor.ServletRequestAware;
import org.springframework.beans.factory.annotation.Autowired;

import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.StopMonitoringDeliveryStructure;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class StopMonitoringAction extends OneBusAwayNYCActionSupport 
  implements ServletRequestAware {

  private static final long serialVersionUID = 1L;

  @Autowired
  public TransitDataService _transitDataService;

  @Autowired  
  private RealtimeService _realtimeService;

  private Siri _response;
  
  private ServiceAlertsHelper _serviceAlertsHelper = new ServiceAlertsHelper();

  private HttpServletRequest _request;
  
  private Date _now = new Date();

  private String _type = "xml";

  public void setType(String type) {
    _type = type;
  }
  
  @Override
  public String execute() {  
    String directionId = _request.getParameter("DirectionRef");
    
    AgencyAndId stopId = null;
    try {
      stopId = AgencyAndIdLibrary.convertFromString(_request.getParameter("MonitoringRef"));
    } catch (Exception e) {
      stopId = new AgencyAndId(_request.getParameter("OperatorRef"), _request.getParameter("MonitoringRef"));
    }
    
    AgencyAndId routeId = null;
    try {
      routeId = AgencyAndIdLibrary.convertFromString(_request.getParameter("LineRef"));
    } catch (Exception e) {
      routeId = new AgencyAndId(_request.getParameter("OperatorRef"), _request.getParameter("LineRef"));
    }
    
    String detailLevel = _request.getParameter("StopMonitoringDetailLevel");

    int maximumOnwardCalls = 0;        
    if (detailLevel != null && detailLevel.equals("calls")) {
      maximumOnwardCalls = Integer.MAX_VALUE;

      try {
        maximumOnwardCalls = Integer.parseInt(_request.getParameter("MaximumNumberOfCallsOnwards"));
      } catch (NumberFormatException e) {
        maximumOnwardCalls = Integer.MAX_VALUE;
      }
    }

    if(stopId != null && stopId.hasValues()) {
      List<MonitoredStopVisitStructure> visits = 
          _realtimeService.getMonitoredStopVisitsForStop(stopId.toString(), maximumOnwardCalls);

      if((routeId != null && routeId.hasValues()) || directionId != null) {
        List<MonitoredStopVisitStructure> filteredVisits = new ArrayList<MonitoredStopVisitStructure>();

        for(MonitoredStopVisitStructure visit : visits) {
          MonitoredVehicleJourneyStructure journey = visit.getMonitoredVehicleJourney();

          AgencyAndId thisRouteId = AgencyAndIdLibrary.convertFromString(journey.getLineRef().getValue());
          String thisDirectionId = journey.getDirectionRef().getValue();
          
          // user filtering
          if((routeId != null && routeId.hasValues()) && !thisRouteId.equals(routeId))
            continue;
          
          if(directionId != null && !thisDirectionId.equals(directionId))
            continue;
          
          filteredVisits.add(visit);
        }

        visits = filteredVisits;
      }
      
      _response = generateSiriResponse(visits);
    }
    
    return SUCCESS;
  }
  

  private Siri generateSiriResponse(List<MonitoredStopVisitStructure> visits) {
    StopMonitoringDeliveryStructure stopMonitoringDelivery = new StopMonitoringDeliveryStructure();
    stopMonitoringDelivery.setResponseTimestamp(_now);
    
    Calendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTime(_now);
    gregorianCalendar.add(Calendar.MINUTE, 1);
    stopMonitoringDelivery.setValidUntil(gregorianCalendar.getTime());
    
    stopMonitoringDelivery.getMonitoredStopVisit().addAll(visits);

    ServiceDelivery serviceDelivery = new ServiceDelivery();
    serviceDelivery.setResponseTimestamp(_now);
    serviceDelivery.getStopMonitoringDelivery().add(stopMonitoringDelivery);

    _serviceAlertsHelper.addSituationExchangeToSiriForStops(serviceDelivery, visits, _transitDataService);

    Siri siri = new Siri();
    siri.setServiceDelivery(serviceDelivery);
    
    return siri;
  }

  public String getStopMonitoring() {
    try {
      if(_type.equals("xml"))
        return _realtimeService.getSiriXmlSerializer().getXml(_response);
      else
        return _realtimeService.getSiriJsonSerializer().getJson(_response, _request.getParameter("callback"));
    } catch(Exception e) {
      return e.getMessage();
    }
  }

  @Override
  public void setServletRequest(HttpServletRequest request) {
    this._request = request;
  }
  
}