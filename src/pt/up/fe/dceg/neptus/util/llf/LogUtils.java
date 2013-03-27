/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * 200?/??/??
 */
package pt.up.fe.dceg.neptus.util.llf;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jfree.chart.JFreeChart;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.LblBeacon;
import pt.up.fe.dceg.neptus.imc.SonarData;
import pt.up.fe.dceg.neptus.imc.VehicleCommand;
import pt.up.fe.dceg.neptus.imc.VehicleCommand.COMMAND;
import pt.up.fe.dceg.neptus.imc.VehicleCommand.TYPE;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIterator;
import pt.up.fe.dceg.neptus.imc.types.PlanSpecificationAdapter;
import pt.up.fe.dceg.neptus.imc.types.PlanSpecificationAdapter.Transition;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.ManeuverFactory;
import pt.up.fe.dceg.neptus.mp.maneuvers.Elevator;
import pt.up.fe.dceg.neptus.mp.maneuvers.FollowPath;
import pt.up.fe.dceg.neptus.mp.maneuvers.FollowTrajectory;
import pt.up.fe.dceg.neptus.mp.maneuvers.Goto;
import pt.up.fe.dceg.neptus.mp.maneuvers.IMCSerialization;
import pt.up.fe.dceg.neptus.mp.maneuvers.Loiter;
import pt.up.fe.dceg.neptus.mp.maneuvers.PopUp;
import pt.up.fe.dceg.neptus.mp.maneuvers.RowsManeuver;
import pt.up.fe.dceg.neptus.mp.maneuvers.StationKeeping;
import pt.up.fe.dceg.neptus.mp.maneuvers.Unconstrained;
import pt.up.fe.dceg.neptus.mp.maneuvers.YoYo;
import pt.up.fe.dceg.neptus.mra.LogMarker;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.plugins.oplimits.OperationLimits;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.MarkElement;
import pt.up.fe.dceg.neptus.types.map.PathElement;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.types.mission.MapMission;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

/**
 * @author ZP
 */
/** 
 * @author pdias
 * 
 */
public class LogUtils {

    public enum LogValidity {
       VALID, NO_DIRECTORY, NO_XML_DEFS, NO_VALID_LOG_FILE 
    };
    
    public static LinkedHashMap<String, String> generateStatistics(IMraLogGroup source) {
        if (source.getLog("EstimatedState") == null) {
            return new LinkedHashMap<String, String>();
        }

        IMraLog parser = source.getLog("EstimatedState");
        IMCMessage lastEntry = parser.getLastEntry();
        IMCMessage entry = parser.firstLogEntry(); //parser.nextLogEntry();

        long startMillis = parser.getCurrentEntry().getTimestampMillis(); //source.getLog("EstimatedState").currentTimeMillis();
        long startMillis2 = parser.firstLogEntry().getTimestampMillis();
        
        System.out.println(startMillis + "" + startMillis2);
        double lastTime = 0;

        double maxDepth = 0;
        double avgDepth = entry.getDouble("z");

        double maxRoll = 0;
        double minRoll = 0;

        double maxPitch = 0;
        double minPitch = 0;

        long numStates = 1;

        double avgRoll = entry.getDouble("phi");
        double avgPitch = entry.getDouble("theta");

        double secs = 0;

        double xPos = entry.getDouble("x");
        double yPos = entry.getDouble("y");
        
        double distance = 0;

        IMCMessage prevEntry = null;
        while ((entry = parser.nextLogEntry()) != null) {

            // secs += (Math.floor(lastTime) != Math.floor(lastEntry.getTimestamp())) ? 1 : 0;
            secs += (Math.floor(lastTime) != Math.floor(lastEntry.getTimestamp())) ? 1 : 0;

            if (secs == 3) {
                double newX = entry.getDouble("x"), newY = entry.getDouble("y");
                distance += Point2D.distance(xPos, yPos, newX, newY);
                xPos = newX;
                yPos = newY;
                secs = 0;
            }

            double depth = entry.getDouble("z");
            maxDepth = Math.max(maxDepth, entry.getDouble("z"));
            double phi = entry.getDouble("phi");
            double theta = entry.getDouble("theta");

            maxRoll = Math.max(maxRoll, phi);
            minRoll = Math.min(minRoll, phi);
            maxPitch = Math.max(maxPitch, theta);
            minPitch = Math.min(minPitch, theta);

            avgDepth = (avgDepth * numStates + depth) / (numStates + 1);

            avgRoll = (avgRoll * numStates + phi) / (numStates + 1);
            avgPitch = (avgPitch * numStates + theta) / (numStates + 1);

            numStates++;
            
            prevEntry = entry;
            lastTime = prevEntry.getTimestamp();
        }

        lastTime = parser.getLastEntry().getTimestamp();
        
        LinkedHashMap<String, String> stats = new LinkedHashMap<String, String>();

        long endMillis = (long) (lastTime * 1000.0); //(long) (startMillis + lastTime * 1000.0);

        Date ds = new Date(startMillis);
        Date df = new Date(endMillis);

        stats.put("Vehicle", "" + LogUtils.getVehicle(source));
        stats.put("Mission start time", "" + ds);
        stats.put("Mission end time", "" + df);
        stats.put("Mission duration", DateTimeUtil.milliSecondsToFormatedString(endMillis - startMillis));
        stats.put("Maximum depth", GuiUtils.getNeptusDecimalFormat(2).format(maxDepth) + " m");
        stats.put("Avg depth", GuiUtils.getNeptusDecimalFormat(2).format(avgDepth) + " m");

        stats.put("Roll min/max/amp/avg", GuiUtils.getNeptusDecimalFormat(2).format(Math.toDegrees(minRoll)) + "\u00B0 / "
                + GuiUtils.getNeptusDecimalFormat(2).format(Math.toDegrees(maxRoll)) + "\u00B0 / "
                + GuiUtils.getNeptusDecimalFormat(2).format(Math.toDegrees(maxRoll - minRoll)) + "\u00B0 / "
                + GuiUtils.getNeptusDecimalFormat(2).format(Math.toDegrees(avgRoll)) + "\u00B0");

        stats.put("Pitch min/max/amp/avg", GuiUtils.getNeptusDecimalFormat(2).format(Math.toDegrees(minPitch)) + "\u00B0 / "
                + GuiUtils.getNeptusDecimalFormat(2).format(Math.toDegrees(maxPitch)) + "\u00B0 / "
                + GuiUtils.getNeptusDecimalFormat(2).format(Math.toDegrees(maxPitch - minPitch)) + "\u00B0 / "
                + GuiUtils.getNeptusDecimalFormat(2).format(Math.toDegrees(avgPitch)) + "\u00B0");

        stats.put("Distance travelled", GuiUtils.getNeptusDecimalFormat(2).format(distance) + " m");
        stats.put("Mean velocity",
                GuiUtils.getNeptusDecimalFormat(2).format(distance / ((endMillis - startMillis) / 1000.0)) + " m/s");

        LocationType loc = LogUtils.getHomeRef(source);
        if (loc != null) {
            stats.put("Home Latitude", loc.getLatitudeAsPrettyString());
            stats.put("Home Longitude", loc.getLongitudeAsPrettyString());
        }
        return stats;
    }

    public static Date getStartDate(IMraLogGroup source) {

        if (source.getLog("EstimatedState") == null)
            return null;

        long startMillis = source.getLog("EstimatedState").currentTimeMillis();
        return new Date(startMillis);

    }

    public static Date[] getMessageMinMaxDates(IMraLog msgLog) {
        if (msgLog == null)
            return null;

        long startMillis = msgLog.firstLogEntry().getTimestampMillis();
        long endMillis = msgLog.getLastEntry().getTimestampMillis();
        return new Date[] { new Date(startMillis), new Date(endMillis) };
    }

    public static MissionType generateMission(IMraLogGroup source) {

        MissionType mission = new MissionType();

        // home ref
        LocationType lt = getHomeRef(source);
        if (lt != null) {
            CoordinateSystem cs = new CoordinateSystem();
            cs.setLocation(lt);
            mission.setHomeRef(cs);
        }

        MapType map = new MapType();

        MapMission mm = new MapMission();
        mm.setId(map.getId());
        mm.setMap(map);

        MarkElement start = new MarkElement();
        start.setId("start");
        start.setName("start");
        map.addObject(start);

        LocationType sloc = getStartupPoint(source);
        if (sloc != null)
            start.setCenterLocation(sloc);
        else
            start.setCenterLocation(new LocationType(mission.getHomeRef()));

        mission.addMap(mm);

        TransponderElement[] te = getTransponders(source);
        for (TransponderElement t : te) {
            t.setParentMap(map);
            t.setMapGroup(map.getMapGroup());
            map.addObject(t);
        }

        return mission;
    }
    
    public static LocationType getHomeRef(IMraLogGroup source) {
        IMraLog parser = source.getLog("HomeRef");
        if (parser != null) {
            IMCMessage lastEntry = parser.getLastEntry();
//            System.out.println("-----" + lastEntry);
            double lat = lastEntry.getDouble("lat");
            double lon = lastEntry.getDouble("lon");
            double depth = lastEntry.getDouble("depth");
            lat = Math.toDegrees(lat);
            lon = Math.toDegrees(lon);

            LocationType center = new LocationType();
            center.setLatitude(lat);
            center.setLongitude(lon);
            center.setDepth(depth);
            return center;
        }
        else if (source.getLog("EstimatedState") != null){
            IMCMessage lastEntry = source.getLog("EstimatedState").getLastEntry();
            double lat = lastEntry.getDouble("lat");
            double lon = lastEntry.getDouble("lon");
            double depth = lastEntry.getDouble("depth");
            lat = Math.toDegrees(lat);
            lon = Math.toDegrees(lon);
            
            LocationType center = new LocationType();
            center.setLatitude(lat);
            center.setLongitude(lon);
            center.setDepth(depth);
            
            return center;
        }
        return null;
    }

    public static OperationLimits getOperationLimits(IMraLogGroup source) {
        IMraLog parser = source.getLog("OperationalLimits");
        if (parser == null)
            return null;

        IMCMessage lastEntry = parser.getLastEntry();
        if (lastEntry == null)
            return null;

        OperationLimits limits = new OperationLimits();
        LinkedHashMap<String, Boolean> bitmask = lastEntry.getBitmask("mask");
        if (bitmask.get("MAX_DEPTH"))
            limits.setMaxDepth(lastEntry.getDouble("max_depth"));
        if (bitmask.get("MIN_ALT"))
            limits.setMinAltitude(lastEntry.getDouble("min_altitude"));
        if (bitmask.get("MAX_ALT"))
            limits.setMaxAltitude(lastEntry.getDouble("max_altitude"));
        if (bitmask.get("MIN_SPEED"))
            limits.setMinSpeed(lastEntry.getDouble("min_speed"));
        if (bitmask.get("MAX_SPEED"))
            limits.setMaxSpeed(lastEntry.getDouble("max_speed"));
        if (bitmask.get("MAX_VRATE"))
            limits.setMaxVertRate(lastEntry.getDouble("max_vrate"));
        if (bitmask.get("AREA")) {
            limits.setOpAreaLat(Math.toDegrees(lastEntry.getDouble("lat")));
            limits.setOpAreaLon(Math.toDegrees(lastEntry.getDouble("lon")));
            limits.setOpRotationRads(lastEntry.getDouble("orientation"));
            limits.setOpAreaWidth(lastEntry.getDouble("width"));
            limits.setOpAreaLength(lastEntry.getDouble("length"));
        }
        return limits;
    }
    
    public static LocationType getStartupPoint(IMraLogGroup source, int src) {
        IMraLog parser = source.getLog("NavigationStartupPoint");
        if (parser != null) {
            IMCMessage entry = parser.getCurrentEntry();
            while (entry != null) {
                if (entry.getHeader().getInteger("src") != src) {
                    entry = parser.nextLogEntry();
                    continue;
                }
                double lat = entry.getDouble("lat");
                double lon = entry.getDouble("lon");
                double depth = entry.getDouble("depth");
                lat = Math.toDegrees(lat);
                lon = Math.toDegrees(lon);

                LocationType center = new LocationType();
                center.setLatitude(lat);
                center.setLongitude(lon);
                center.setDepth(depth);
                return center;
            }
        }

        return null;
    }
    
    public static LocationType getStartupPoint(IMraLogGroup source) {
        IMraLog parser = source.getLog("NavigationStartupPoint");
        if (parser != null) {
            IMCMessage lastEntry = parser.getLastEntry();

            double lat = lastEntry.getDouble("lat");
            double lon = lastEntry.getDouble("lon");
            double depth = lastEntry.getDouble("depth");
            lat = Math.toDegrees(lat);
            lon = Math.toDegrees(lon);

            LocationType center = new LocationType();
            center.setLatitude(lat);
            center.setLongitude(lon);
            center.setDepth(depth);
            return center;
        }
        return null;
    }

    public static TransponderElement[] getTransponders(IMraLogGroup source) {
        IMraLog parser = source.getLog("LblConfig");
        if (parser == null)
            return new TransponderElement[0];
        
        Vector<TransponderElement> transp = new Vector<TransponderElement>();
        
        try {
            IMCMessage config = parser.getLastEntry();
            
            if(config.getMessageList("beacons") != null) {
                for (IMCMessage lblBeacon : config.getMessageList("beacons")) {
                    String beacon = lblBeacon.getString("beacon");
                    double lat = Math.toDegrees(lblBeacon.getDouble("lat"));
                    double lon = Math.toDegrees(lblBeacon.getDouble("lon"));
                    double depth = lblBeacon.getDouble("depth");
                    TransponderElement el = new TransponderElement();
                    LocationType lt = new LocationType();
                    lt.setLatitude(lat);
                    lt.setLongitude(lon);
                    lt.setDepth(depth);
                    el.setId(beacon);
                    el.setName(beacon);
                    el.setCenterLocation(lt);
                    transp.add(el);
                }
            } 
            else {
                for (int i = 0; i < 6; i++) {
                    IMCMessage msg = config.getMessage("beacon" + i);
                    if (msg == null)
                        continue;
                    LblBeacon lblBeacon = LblBeacon.clone(msg);
                    String beacon = lblBeacon.getBeacon();
                    double lat = Math.toDegrees(lblBeacon.getLat());
                    double lon = Math.toDegrees(lblBeacon.getLon());
                    double depth = lblBeacon.getDepth();
                    TransponderElement el = new TransponderElement();
                    LocationType lt = new LocationType();
                    lt.setLatitude(lat);
                    lt.setLongitude(lon);
                    lt.setDepth(depth);
                    el.setId(beacon);
                    el.setName(beacon);
                    el.setCenterLocation(lt);
                    transp.add(el);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();            
        }
        return transp.toArray(new TransponderElement[0]);
    }

    public static boolean isValidLogFolder(File dir) {
        if (!dir.isDirectory() || !dir.canRead())
            return false;

        for (File f : dir.listFiles()) {
            if (FileUtil.getFileExtension(f).equalsIgnoreCase("llf"))
                return f.canRead();
            if (f.getName().equalsIgnoreCase("data.bsf"))
                return f.canRead();
        }
        return false;
    }

    public static boolean isValidZipSource(File zipFile) {
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                // if (ze.getName().equals("EstimatedState.llf"))
                if (FileUtil.getFileExtension(ze.getName()).equalsIgnoreCase("llf")) {
                    zis.close();
                    return true;
                }
                ze = zis.getNextEntry();
            }
            zis.close();
        }
        catch (Exception e) {
        }
        return false;
    }

    public static LogValidity isValidLSFSource(File dir) {
        if (!dir.isDirectory() || !dir.canRead())
            return LogValidity.NO_DIRECTORY;

        short lsfFx = 0, lsfGzFx = 0, lsfBZip2Fx = 0, defXmlFx = 0;
        for (File f : dir.listFiles()) {
            if (FileUtil.getFileExtension(f).equalsIgnoreCase("lsf"))
                lsfFx++;
            if (FileUtil.getFileExtension(f).equalsIgnoreCase("xml"))
                defXmlFx++;
            if (FileUtil.getFileExtension(f).equalsIgnoreCase("gz")) {
                String fex = FileUtil.getFileExtension(f.getName().substring(0, f.getName().length() - 3)) + ".gz";
                if (fex.equalsIgnoreCase("lsf.gz"))
                    lsfGzFx++;
            }
            if (FileUtil.getFileExtension(f).equalsIgnoreCase("bz2")) {
                String fex = FileUtil.getFileExtension(f.getName().substring(0, f.getName().length() - 4)) + ".bz2";
                if (fex.equalsIgnoreCase("lsf.bz2"))
                    lsfBZip2Fx++;
            }
        }
        if ((lsfFx + lsfGzFx + lsfBZip2Fx) > 0 && defXmlFx > 0)
            return LogValidity.VALID;
        
        if ((lsfFx + lsfGzFx + lsfBZip2Fx) == 0)
            return LogValidity.NO_VALID_LOG_FILE;
        
        return LogValidity.NO_XML_DEFS;
    }

    /**
     * @author zp
     * @param mt
     * @param source
     * @return
     */
    public static PlanType generatePlan(MissionType mt, IMraLogGroup source) {
        try {
            IMraLog log = source.getLog("PlanSpecification");
            if (log == null || log.getNumberOfEntries() > 1)
                return generatePlanFromVehicleCommands(mt, source);
            
            PlanSpecificationAdapter imcPlan = new PlanSpecificationAdapter(log.getLastEntry());
            
            
            PlanType plan = new PlanType(mt);
            plan.setId(imcPlan.getPlanId());
            
            for (String manId : imcPlan.getAllManeuvers().keySet()) {
                IMCMessage maneuver = imcPlan.getAllManeuvers().get(manId);
                Maneuver man = parseManeuver(manId, maneuver);
                plan.getGraph().addManeuver(man);
            }

            for (Transition imcTransition: imcPlan.getAllTransitions()) {
                plan.getGraph().addTransition(imcTransition.getSourceManeuver(), imcTransition.getDestManeuver(), imcTransition.getConditions());                
            }
            
            plan.getGraph().setInitialManeuver(imcPlan.getFirstManeuverId());
            
            return plan;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * @author zp
     * @param mt
     * @param source
     * @return
     */
    public static PlanType generatePlanFromVehicleCommands(MissionType mt, IMraLogGroup source) {
        try {
            IMraLog log = source.getLog("VehicleCommand");
            
            if (log == null)
                return null;
            
            PlanType pt = new PlanType(mt);
            pt.setId("Executed");

            IMCMessage msg = log.nextLogEntry();
            
            int count = 1;
            
            while (msg != null) {
                VehicleCommand cmd = VehicleCommand.clone(msg);
                if (cmd.getType() == TYPE.REQUEST && cmd.getCommand() == COMMAND.EXEC_MANEUVER) {
                    IMCMessage maneuver = cmd.getManeuver();
                    if (maneuver != null) {
                        String id = ""+(count++);
                        Maneuver man = parseManeuver(id, maneuver);
                        pt.getGraph().addManeuver(man);   
                    }
                }
                
                msg = log.nextLogEntry();
                
            }
            
            return pt;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    
    /**
     * See {@link IMCUtils#parseManeuver(IMCMessage)}
     */
    protected static Maneuver parseManeuver(String manId, IMCMessage msg) {
        String manType = msg.getAbbrev();
        Maneuver maneuver = null;
        if ("Goto".equalsIgnoreCase(manType))
            maneuver = new Goto();
        else if ("Popup".equalsIgnoreCase(manType))
            maneuver = new PopUp();
        else if ("Loiter".equalsIgnoreCase(manType))
            maneuver = new Loiter();
        else if ("Teleoperation".equalsIgnoreCase(manType))
            maneuver = new Unconstrained();
        else if ("Rows".equalsIgnoreCase(manType))
            maneuver = new RowsManeuver();
        else if ("FollowTrajectory".equalsIgnoreCase(manType))
            maneuver = new FollowTrajectory();
        else if ("FollowPath".equalsIgnoreCase(manType))
            maneuver = FollowPath.createFollowPathOrPattern(msg);
        else if ("StationKeeping".equalsIgnoreCase(manType))
            maneuver = new StationKeeping();
        else if ("Elevator".equalsIgnoreCase(manType))
            maneuver = new Elevator();
        else if ("YoYo".equalsIgnoreCase(manType))
            maneuver = new YoYo();
        else {
            try {
                maneuver = ManeuverFactory.createManeuver(manType, "pt.up.fe.dceg.neptus.mp.maneuvers." + manType);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (maneuver == null)
            return null;
        maneuver.setId(manId);

        if (maneuver instanceof IMCSerialization)
            ((IMCSerialization) maneuver).parseIMCMessage(msg);

        return maneuver;

    }

    public static String parseInlineName(String data) {
        if (data.startsWith("%INLINE{")) {
            return data.substring("%INLINE{".length(), data.length() - 1);
        }
        return null;
    }

    public static LocationType getLocation(IMCMessage estimatedStateMessage) {
        try {
            if (estimatedStateMessage != null) {
                LocationType loc = new LocationType();

                // 0 -> NED ONLY, 1 -> LLD ONLY, 2 -> NED_LLD
                long refMode = estimatedStateMessage.getLong("ref");
                
                // IMC5 Compatibility
                if(!estimatedStateMessage.getMessageType().getFieldNames().contains("ref")) {
                    refMode = 2;
                }
                
                if (refMode == 1 || refMode == 2) {
                    loc.setLatitude(Math.toDegrees(estimatedStateMessage.getDouble("lat")));
                    loc.setLongitude(Math.toDegrees(estimatedStateMessage.getDouble("lon")));
                    loc.setDepth(estimatedStateMessage.getDouble("depth"));
                }
                if (refMode == 0 || refMode == 2) {
                    loc.translatePosition(estimatedStateMessage.getDouble("x"), estimatedStateMessage.getDouble("y"),
                            estimatedStateMessage.getDouble("z"));
                }
                loc.convertToAbsoluteLatLonDepth();
                return loc;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    

    /**
     * @param estimatedStateEntry
     * 
     */
    public static LocationType getLocation(LocationType baseLoc, IMCMessage estimatedStateEntry) {
        LocationType loc = getLocation(estimatedStateEntry);
        if (loc == null)
            return null;

        long refMode = estimatedStateEntry.getLong("ref");
        if (refMode == 0) {
            loc.setLatitude(baseLoc.getLatitudeAsDoubleValue());
            loc.setLongitude(baseLoc.getLongitudeAsDoubleValue());
            loc.setDepth(baseLoc.getDepth());
        }
        return loc;
    }

    public static PathElement generatePath(MissionType mission, IMraLogGroup source) {
        MapType mt = new MapType();
        LocationType lt = new LocationType(mission.getStartLocation());
        PathElement pe = new PathElement(MapGroup.getMapGroupInstance(mission), mt, lt);
        pe.setParentMap(mt);
        mt.addObject(pe);

        IMraLog parser = source.getLog("EstimatedState");
        if (parser == null)
            return pe;

        IMCMessage entry = parser.nextLogEntry();

        // entry = parser.getEntryAfter(11.0);
        LocationType tmp = new LocationType();

        while (entry != null) {
            parser.advance(100);
            entry = parser.nextLogEntry();
            if (entry != null) {
                long refMode = entry.getLong("ref");
                if (refMode == 0) {
                    pe.addPoint(entry.getDouble("y"),entry.getDouble("x"), entry.getDouble("z"),false);
                }
                else if (refMode == 1) {
                    double lat = entry.getDouble("lat");
                    double lon = entry.getDouble("lon");
                    double depth = entry.getDouble("depth");

                    if (lat != tmp.getLatitudeAsDoubleValue() && lon != tmp.getLongitudeAsDoubleValue()) {
                        tmp.setLatitude(Math.toDegrees(lat));
                        tmp.setLongitude(Math.toDegrees(lon));
                        tmp.setDepth(depth);
                        double[] offs = tmp.getOffsetFrom(mission.getStartLocation());
                        pe.addPoint(offs[1], offs[0], offs[2], false);
                    }
                }
                else if (refMode == 2) {
                    double lat = entry.getDouble("lat");
                    double lon = entry.getDouble("lon");
                    double depth = entry.getDouble("depth");
                    double x = entry.getDouble("x");
                    double y = entry.getDouble("y");
                    double z = entry.getDouble("z");

                    // if (lat != tmp.getLatitudeAsDoubleValue() && lon != tmp.getLongitudeAsDoubleValue()
                    // && depth != tmp.getDepth()) {
                    tmp.setLatitude(Math.toDegrees(lat));
                    tmp.setLongitude(Math.toDegrees(lon));
                    tmp.setDepth(depth);
                    // }
                    double[] offs = tmp.getOffsetFrom(mission.getStartLocation());
                    // if (!(xVals.contains(offs[0]+x) && yVals.contains(offs[1]+y))) {
                    // xVals.add(offs[0]+x);
                    // yVals.add(offs[1]+y);
                    // }
                    pe.addPoint(offs[1] + y, offs[0] + x, offs[2] + z, false);
                }
            }
        }
        pe.setMyColor(Color.green);
        pe.setFinished(true);

        MapMission mm = new MapMission();
        mm.setId(mt.getId());
        mm.setMap(mt);
        mission.addMap(mm);
        MapGroup.getMapGroupInstance(mission).addMap(mt);

        return pe;
    }

    public static PathElement generatePath(MissionType mission, Vector<LocationType> locations) {
        MapType mt = new MapType();
        LocationType first = locations.firstElement();
        locations.remove(0);
        // LocationType lt = new LocationType(mission.getHomeRef());
        PathElement pe = new PathElement(MapGroup.getMapGroupInstance(mission), mt, first);
        pe.setParentMap(mt);
        mt.addObject(pe);

        for (LocationType l : locations) {
            double[] offsets = l.getOffsetFrom(first);
            pe.addPoint(offsets[0], offsets[1], offsets[2], false);

        }
        pe.setMyColor(Color.green);
        pe.setFinished(true);

        MapMission mm = new MapMission();
        mm.setId(mt.getId());
        mm.setMap(mt);
        mission.addMap(mm);
        MapGroup.getMapGroupInstance(mission).addMap(mt);

        return pe;
    }

    public static VehicleType getVehicle(IMraLogGroup source) {

        // logs that won't be logged by external systems
        String privateLogs[] = new String[] { "Voltage", "CpuUsage", "Temperature" };

        for (int i = 0; i < privateLogs.length; i++) {
            IMraLog log = source.getLog(privateLogs[i]);
            if (log != null) {
                IMCMessage msg = log.nextLogEntry();
                if (msg == null)
                    continue;
                int src_id = msg.getHeader().getInteger("src");

                VehicleType vt = VehiclesHolder.getVehicleWithImc(new ImcId16(src_id));

                if (vt != null)
                    return vt;
            }
        }

        return null;
    }

    public static void saveCharAsPdf(JFreeChart chart, File outFile) {
        Rectangle pageSize = new Rectangle(1024, 768);
        try {
            FileOutputStream out = new FileOutputStream(outFile);

            Document doc = new Document(pageSize);

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPdfVersion('1');

            doc.open();

            PdfContentByte cb = writer.getDirectContent();

            int width = (int) pageSize.getWidth();
            int height = (int) pageSize.getHeight();

            PdfTemplate tp = cb.createTemplate(width, height);

            java.awt.Graphics2D g2 = tp.createGraphicsShapes(width, height);
            chart.draw(g2, new Rectangle2D.Double(0, 0, width, height));
            // chart.paint(g2);

            g2.dispose();
            cb.addTemplate(tp, 0, 0);

            doc.close();
            out.flush();
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void savePlanAsPdf(StateRenderer2D renderer, File outFile) {
        Rectangle pageSize = new Rectangle(800, 600);
        try {
            FileOutputStream out = new FileOutputStream(outFile);

            Document doc = new Document(pageSize);

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPdfVersion('1');

            doc.open();

            PdfContentByte cb = writer.getDirectContent();

            int width = (int) pageSize.getWidth();
            int height = (int) pageSize.getHeight();

            PdfTemplate tp = cb.createTemplate(width, height);

            java.awt.Graphics2D g2 = tp.createGraphicsShapes(width, height);
            renderer.setSize(width, height);
            renderer.update(g2);
            // chart.draw(g2, new Rectangle2D.Double(0,0,width,height));
            // chart.paint(g2);

            g2.dispose();
            cb.addTemplate(tp, 0, 0);

            doc.close();
            out.flush();
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static LinkedHashMap<Integer, String> getEntities(IMraLogGroup source) {
        LinkedHashMap<Integer, String> entities = new LinkedHashMap<Integer, String>();

        entities.put(255, "Unknown");

        IMraLog parser = source.getLog("EntityInfo");

        if (parser != null) {
            IMCMessage entry = parser.nextLogEntry();

            while (entry != null) {
                try {
                    entities.put(entry.getInteger("id"), entry.getString("label"));
                }
                catch (Exception e) {
                }
                entry = parser.nextLogEntry();
            }
        }

        return entities;
    }
    /**
     * Returns the entity map for a given IMC node source id
     * @param source Log source
     * @param srcId Source id number 
     * @return The entity map
     */
    public static LinkedHashMap<Integer, String> getEntities(IMraLogGroup source, int srcId) {
        LinkedHashMap<Integer, String> entities = new LinkedHashMap<Integer, String>();

        entities.put(255, "Unknown");

        IMraLog parser = source.getLog("EntityInfo");

        if (parser != null) {
            IMCMessage entry = parser.nextLogEntry();

            while (entry != null) {
                if(entry.getHeader().getInteger("src")==srcId) {
                    try {
                        entities.put(entry.getInteger("id"), entry.getString("label"));
                    }
                    catch (Exception e) {
                    }
                }
                entry = parser.nextLogEntry();
            }
        }
        return entities;
    }
    
    
    public static ArrayList<LogMarker> getMarkersFromSource(IMraLogGroup source) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(source.getFile("Data.lsf").getParent()+"/marks.dat"));
            @SuppressWarnings("unchecked")
            ArrayList<LogMarker> markers = (ArrayList<LogMarker>)ois.readObject();
            ois.close();
            return markers;
        } catch(Exception e) {
            return new ArrayList<>();
        }
    }
   
    public static boolean hasIMCSidescan(IMraLogGroup source) {
        LsfIndex index = source.getLsfIndex();
        LsfIterator<SonarData> it = index.getIterator(SonarData.class);
        SonarData sd = it.next();
        long ts = sd.getTimestampMillis();
        while((sd.getTimestampMillis() - ts) < 5000) {
            if(sd.getType() == SonarData.TYPE.SIDESCAN)
                return true;
            sd = it.next();
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println(parseInlineName("%INLINE{Goto}"));
        System.out.println(parseInlineName("%INLINE{Popup}"));
    }
}
