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


var VehicleStatus = Ember.Application.create({
	ready: function() {
		$("#menu").tabs();
	}
		
});

/******************* Views ************************************/
VehicleStatus.VehicleView = Ember.View.extend({
	tagName: "table",
	didInsertElement: function() {
		var controller = this.get('controller');
		controller.loadGridData();
	},
	controllerBinding: "VehicleStatus.VehiclesController"
});

VehicleStatus.FilterView = Ember.View.extend({
	tagName: "ul",
	didInsertElement: function() {
		var controller = this.get('controller');
		controller.loadFiltersData();
	},
	controllerBinding: "VehicleStatus.FiltersController"
});

VehicleStatus.ParametersView = Ember.View.extend({
	

});

/******************* Controllers ************************************/
VehicleStatus.ParametersController = Ember.ArrayController.create({
	content: [],
});

VehicleStatus.VehiclesController = Ember.ArrayController.create({
	content: [],
	loadGridData : function() {
		$("#vehicleGrid").jqGrid({
			url: "vehicle-status!getVehicleData.action?ts=" + new Date().getTime(),
			datatype: "json",
			mType: "GET",
			colNames: ["Status","Vehicle Id", "Last Update", "Inferred State", "Inferred DSC, Route + Direction", "Observed DSC", "Pull-out", "Pull-in", "Details"],
			colModel:[ {name:'status',index:'status', width:70, sortable:false}, 
			           {name:'vehicleId',index:'vehicleId', width:70}, 
			           {name:'lastUpdateTime',index:'lastUpdateTime', width:70}, 
			           {name:'inferredState',index:'inferredState', width:100, sortable:false}, 
			           {name:'inferredDestination',index:'inferredDestination', width:170, sortable:false}, 
			           {name:'observedDSC',index:'observedDSC', width:80}, 
			           {name:'pulloutTime',index:'pulloutTime', width:70},
			           {name:'pullinTime',index:'pullinTime', width:70},
			           {name:'details',index:'details', width:65}
			         ],
			height: "390",
			width: "670",
			//width: "auto",
			viewrecords: true,
			loadonce:false,
			jsonReader: {
				root: "rows",
			    page: "page",
			    total: "total",
			    records: "records",
				repeatitems: false
			},
			pager: "#pager"
		}).navGrid("#pager", {edit:false,add:false,del:false});
	}
});

VehicleStatus.FiltersController = Ember.ArrayController.create({
	content: [],
	loadFiltersData : function() {
		$.ajax({
			type: "GET",
			url: "../../filters/vehicle-filters.xml",
			dataType: "xml",
			success: function(xml) {
				//Add depot options
				$(xml).find("Depot").each(function(){
					$("#depot").append("<option value=\"" +$(this).text() + "\"" + ">" +$(this).text() + "</option>");
				});
				//Add inferred state options
				$(xml).find("InferredState").each(function(){
					$("#inferredState").append("<option value=\"" +$(this).text() + "\"" + ">" +$(this).text() + "</option>");
				});
				//Add pullout options
				$(xml).find("PulloutStatus").each(function(){
					$("#pulloutStatus").append("<option value=\"" +$(this).text() + "\"" + ">" +$(this).text() + "</option>");
				});
			},
			error: function(request) {
				alert("Error: " + request.statusText);
			}
		});
	}	
});

/******************* Model ************************************/