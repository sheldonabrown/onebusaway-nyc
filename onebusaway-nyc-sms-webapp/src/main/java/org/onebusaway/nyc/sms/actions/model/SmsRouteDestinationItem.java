/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.sms.actions.model;

import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import java.util.List;

public class SmsRouteDestinationItem extends RouteDestinationItem {

  private List<NaturalLanguageStringBean> serviceAlerts;
  
  private List<String> distanceAwayStrings;
  
  public SmsRouteDestinationItem(StopGroupBean group, List<StopResult> stops, Boolean hasUpcomingScheduledService) {
    super(group, stops, hasUpcomingScheduledService);
  }

  public List<NaturalLanguageStringBean> getServiceAlerts() {
    return serviceAlerts;
  }

  public void setServiceAlerts(List<NaturalLanguageStringBean> serviceAlerts) {
    this.serviceAlerts = serviceAlerts;
  }

  public List<String> getDistanceAwayStrings() {
    return distanceAwayStrings;
  }

  public void setDistanceAwayStrings(List<String> distanceAwayStrings) {
    this.distanceAwayStrings = distanceAwayStrings;
  }
  
}
