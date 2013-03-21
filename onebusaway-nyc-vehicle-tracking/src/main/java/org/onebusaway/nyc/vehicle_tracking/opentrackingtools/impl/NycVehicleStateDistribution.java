package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;

import gov.sandia.cognition.math.matrix.Matrix;
import gov.sandia.cognition.math.matrix.Vector;
import gov.sandia.cognition.statistics.DataDistribution;
import gov.sandia.cognition.statistics.distribution.InverseWishartDistribution;
import gov.sandia.cognition.statistics.distribution.MultivariateGaussian;

import org.opentrackingtools.VehicleStateInitialParameters;
import org.opentrackingtools.distributions.CountedDataDistribution;
import org.opentrackingtools.distributions.DeterministicDataDistribution;
import org.opentrackingtools.distributions.OnOffEdgeTransDistribution;
import org.opentrackingtools.distributions.OnOffEdgeTransPriorDistribution;
import org.opentrackingtools.distributions.PathStateDistribution;
import org.opentrackingtools.distributions.PathStateMixtureDensityModel;
import org.opentrackingtools.graph.InferenceGraph;
import org.opentrackingtools.model.GpsObservation;
import org.opentrackingtools.model.SimpleBayesianParameter;
import org.opentrackingtools.model.VehicleStateDistribution;
import org.opentrackingtools.paths.PathEdge;
import org.opentrackingtools.paths.PathState;
import org.opentrackingtools.util.model.TransitionProbMatrix;

import java.util.Random;

public class NycVehicleStateDistribution extends
    VehicleStateDistribution<Observation> {
  
  public static class NycVehicleStateDistributionFactory extends
      VehicleStateDistributionFactory<Observation, NycTrackingGraph> {

    @Override
    public VehicleStateDistribution<Observation> createInitialVehicleState(
        VehicleStateInitialParameters parameters, NycTrackingGraph graph,
        Observation obs, Random rng, PathEdge pathEdge) {
      
      NycVehicleStateDistribution nycVehicleStateDist = 
          new NycVehicleStateDistribution(super.createInitialVehicleState(parameters, graph, obs, rng, pathEdge));
      
      RunStateEstimator runStateEstimator = new RunStateEstimator(graph, obs, 
          nycVehicleStateDist.getPathStateParam().getParameterPrior(), null, rng);
      
      CountedDataDistribution<RunState> newRunStateDist = runStateEstimator.createInitialLearnedObject();
      
      /*
       * Again, we choose to use the max value key.
       */
      final DeterministicDataDistribution<RunState> initialPriorDist = 
          new DeterministicDataDistribution<RunState>(newRunStateDist.getMaxValueKey());
      
      nycVehicleStateDist.setRunStateParam(SimpleBayesianParameter.create(
          newRunStateDist.getMaxValueKey(), 
          (DataDistribution<RunState>)newRunStateDist, 
          (DataDistribution<RunState>)initialPriorDist));
      
      return nycVehicleStateDist;
    }

  }

  private static final long serialVersionUID = 355318894093095008L;
  
  protected SimpleBayesianParameter<RunState, DataDistribution<RunState>, DataDistribution<RunState>>
    runStateParam;

  protected NycVehicleStateDistribution(VehicleStateDistribution<Observation> other) {
    super(other);
    this.runStateParam = null;
  }
  
  public NycVehicleStateDistribution(NycVehicleStateDistribution other) {
    super(other);
    this.runStateParam = other.runStateParam;
  }

  public NycVehicleStateDistribution(
      InferenceGraph inferredGraph,
      Observation observation,
      SimpleBayesianParameter<Vector, MultivariateGaussian, MultivariateGaussian> motionStateParam,
      SimpleBayesianParameter<PathState, PathStateMixtureDensityModel, PathStateDistribution> pathStateParam,
      SimpleBayesianParameter<Matrix, MultivariateGaussian, InverseWishartDistribution> observationCovParam,
      SimpleBayesianParameter<Matrix, MultivariateGaussian, InverseWishartDistribution> onRoadModelCovParam,
      SimpleBayesianParameter<Matrix, MultivariateGaussian, InverseWishartDistribution> offRoadModelCovParam,
      SimpleBayesianParameter<TransitionProbMatrix, OnOffEdgeTransDistribution, OnOffEdgeTransPriorDistribution> edgeTransitionDist,
      SimpleBayesianParameter<RunState, DataDistribution<RunState>, DataDistribution<RunState>> runStateParam,
      VehicleStateDistribution<Observation> parentState) {
    super(inferredGraph, observation, motionStateParam, pathStateParam,
        observationCovParam, onRoadModelCovParam, offRoadModelCovParam,
        edgeTransitionDist, parentState);
    this.runStateParam = runStateParam;
  }

  public SimpleBayesianParameter<RunState, DataDistribution<RunState>, DataDistribution<RunState>> getRunStateParam() {
    return runStateParam;
  }

  public void setRunStateParam(
      SimpleBayesianParameter<RunState, DataDistribution<RunState>, DataDistribution<RunState>> runStateParam) {
    this.runStateParam = runStateParam;
  }
  
}