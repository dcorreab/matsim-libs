package org.matsim.core.mobsim.qsim;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.utils.misc.Time;

public class ActivityEngine implements MobsimEngine, ActivityHandler {

	private class AgentEntry {
		public AgentEntry(MobsimAgent agent, double activityEndTime) {
			this.agent = agent;
			this.activityEndTime = activityEndTime;
		}
		MobsimAgent agent;
		double activityEndTime;
	}

	/**
	 * This list needs to be a "blocking" queue since this is needed for
	 * thread-safety in the parallel qsim. cdobler, oct'10
	 */
	private Queue<AgentEntry> activityEndsList = new PriorityBlockingQueue<AgentEntry>(500, new Comparator<AgentEntry>() {

		@Override
		public int compare(AgentEntry arg0, AgentEntry arg1) {
			int cmp = Double.compare(arg0.activityEndTime, arg1.activityEndTime);
			if (cmp == 0) {
				// Both depart at the same time -> let the one with the larger id be first (=smaller)
				return arg1.agent.getId().compareTo(arg0.agent.getId());
			}
			return cmp;
		}

	});

	private InternalInterface internalInterface;

	@Override
	public void onPrepareSim() {
		// Nothing to do here
	}

	@Override
	public void doSimStep(double time) {
		while (activityEndsList.peek() != null) {
			MobsimAgent agent = activityEndsList.peek().agent;
			if (agent.getActivityEndTime() <= time) {
				activityEndsList.poll();
				unregisterAgentAtActivityLocation(agent);
				agent.endActivityAndComputeNextState(time);
				internalInterface.arrangeNextAgentState(agent) ;
			} else {
				return;
			}
		}
	}

	@Override
	public void afterSim() {
		double now = this.internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		for (AgentEntry entry : activityEndsList) {
			if (entry.activityEndTime!=Double.POSITIVE_INFINITY && entry.activityEndTime!=Time.UNDEFINED_TIME) {
				// since we are at an activity, it is not plausible to assume that the agents know mode or destination 
				// link id.  Thus generating the event with ``null'' in the corresponding entries.  kai, mar'12
				EventsManager eventsManager = internalInterface.getMobsim().getEventsManager();
				eventsManager.processEvent(eventsManager.getFactory().createAgentStuckEvent(now, entry.agent.getId(),null, null));
			}
		}
		activityEndsList.clear();
	}

	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	@Override
	public boolean handleActivity(MobsimAgent agent) {
		activityEndsList.add(new AgentEntry(agent, agent.getActivityEndTime()));
		internalInterface.registerAdditionalAgentOnLink(agent);
		if ( agent.getActivityEndTime()==Double.POSITIVE_INFINITY ) {
			internalInterface.getMobsim().getAgentCounter().decLiving() ;
		}
		return true;
	}


	/**
	 * For within-day replanning. Tells this engine that the activityEndTime the agent reports may have changed since 
	 * the agent was added to this engine through handleActivity.
	 * May be merged with handleActivity, since this engine can know by itself if it was called the first time
	 * or not.
	 * 
	 * @param agent The agent.
	 */
	void rescheduleActivityEnd(final MobsimAgent agent) {
		double newActivityEndTime = agent.getActivityEndTime();
		AgentEntry oldEntry = removeAgentFromQueue(agent);

		// The intention in the following is that an agent that is no longer alive has an activity end time of infinity.  The number of
		// alive agents is only modified when an activity end time is changed between a finite time and infinite.  kai, jun'11
		if (oldEntry == null) {
			if (newActivityEndTime == Double.POSITIVE_INFINITY) {
				// agent was de-activated and still should be de-activated - nothing to do here
			} else {
				// re-activate the agent
				activityEndsList.add(new AgentEntry(agent, newActivityEndTime));
				internalInterface.registerAdditionalAgentOnLink(agent);
				((AgentCounter) internalInterface.getMobsim().getAgentCounter()).incLiving();				
			}
		} else if (newActivityEndTime == Double.POSITIVE_INFINITY) {
			/*
			 * After the re-planning the agent's current activity has changed to its last activity.
			 * Therefore the agent is de-activated. cdobler, oct'11
			 */
			unregisterAgentAtActivityLocation(agent);
			internalInterface.getMobsim().getAgentCounter().decLiving();
		} else {
			/*
			 *  The activity is just rescheduled during the day, so we keep the agent active. cdobler, oct'11
			 */
			activityEndsList.add(new AgentEntry(agent, newActivityEndTime));
		}
	}

	/**
	 * Returns the next time an Activity ends. This is only used in the initialization of the QSim to know when to start
	 * the simulation timer. I plan to remove this and have the QSim itself look at the initial agent population and
	 * decide for itself.
	 * 
	 * @return The next time and Activity ends.
	 */
	Double getNextActivityEndTime() {
		AgentEntry entry = activityEndsList.peek();
		if (entry != null) {
			double nextActivityEndTime = entry.agent.getActivityEndTime();
			return nextActivityEndTime;
		} else {
			return null;
		}
	}

	private AgentEntry removeAgentFromQueue(MobsimAgent agent) {
		Iterator<AgentEntry> iterator = activityEndsList.iterator();
		while (iterator.hasNext()) {
			AgentEntry entry = iterator.next();
			if (entry.agent == agent) {
				iterator.remove();
				return entry;
			}
		}
		return null;
	}

	private void unregisterAgentAtActivityLocation(final MobsimAgent agent) {
		Id agentId = agent.getId();
		Id linkId = agent.getCurrentLinkId();
		internalInterface.unregisterAdditionalAgentOnLink(agentId, linkId);
	}

}