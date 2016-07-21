/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: tsmarques
 * 29 Feb 2016
 */
package pt.lsts.neptus.plugins.mvplanning.interfaces;

import java.util.Map;
import java.util.concurrent.Future;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager.SendResult;
import pt.lsts.neptus.console.ConsoleSystem;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author tsmarques
 *
 */
public interface ConsoleAdapter {
    /**
     * Register the given object to the console's event bus
     * */
    void registerToEventBus(Object obj);

    /**
     * Unregister the given object to the console's event bus
     * */
    void unregisterToEventBus(Object obj);


    /**
     * Allow the given object to subscribe to
     * to the messages bus and receive IMC
     * messages
     * */
    void subscribeToIMCMessages(Object obj);


    /**
     * Unsubscribe from IMC messages's bus
     * */
    void unsubscribeToIMCMessages(Object obj);


    /**
     * Post an event to the console's event bus
     * */
    void post(Object event);


    /**
     * Send an IMC message to a given system
     * */
    boolean sendMessage(String dest, IMCMessage msg);


    /**
     * Same as {@link #sendMessage(String, IMCMessage)} but
     * uses TCP instead.
     * */
    Future<SendResult> sendMessageReliably(String dest, IMCMessage message);


    /**
     * Get the console systems
     * */
    Map<String, ConsoleSystem> getSystems();


    /**
     * Get obstacles drawn on the console's map
     * */
    AbstractElement[] getMapObstacles();


    /**
     * Get the map group of the console's current map
     * */
    MapGroup getMapGroup();


    /**
     * Get console's current mission
     * */
    MissionType getMission();

    /**
     * Add a plan to the console's mission
     * */
    void addPlanToMission(PlanType plan);

    /**
     * Start SwingWorker and save the current console's mission
     * */
    void saveMission();

    /**
     * Posts a success notification
     * */
    void notifiySuccess(String title, String text);

    /**
     * Posts a warning notification
     * */
    void notifyWarning(String title, String text);

    /**
     * Posts an error notification
     * */
    void notifiyError(String title, String text);
}