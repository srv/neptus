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
 * Author: zp
 * Dec 4, 2013
 */
package pt.lsts.neptus.util.bathymetry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author zp
 * 
 */
public class CachedData extends TidePredictionFinder {

    private boolean loading = true;
    private SortedSet<TidePeak> cachedData = null;

    public void loadFile(File f) throws Exception {
        cachedData = new TreeSet<>();
        if (f == null || ! f.canRead())
            throw new Exception("Tides file is not valid: "+f); 
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line = br.readLine();

        while (line != null) {
            if (line.startsWith("#"))
                continue;
            String parts[] = line.split(",");
            long unixTimeSecs = Long.parseLong(parts[2]);
            double height = Double.parseDouble(parts[3]);
            Date d = new Date(unixTimeSecs * 1000);
            //System.out.println(d);
            cachedData.add(new TidePeak(d, height));
            line = br.readLine();
        }
        br.close();
    }
    
    public void saveFile(String port, File f) throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (TidePeak tp : cachedData) {
            writer.write(port+","+sdf.format(tp.date)+","+tp.date.getTime()/1000+","+tp.height+"\n");
        }
        writer.close();
    }
    
    public void update(Vector<TidePeak> newData) {
        Collections.sort(newData);
        // remove all data contained in the new data +/- 5 hours
        TidePeak removeFirst = new TidePeak(new Date(newData.firstElement().date.getTime() - (1000 * 3600 * 5)), 0);
        TidePeak removeLast = new TidePeak(new Date(newData.firstElement().date.getTime() + (1000 * 3600 * 5)), 0);
        Vector<TidePeak> toRemove = new Vector<>();
        toRemove.addAll(cachedData.subSet(removeFirst, removeLast));
        cachedData.removeAll(toRemove);
        cachedData.addAll(newData);
    }

    public boolean contains(Date d) {
        while (loading) {
            try {
                Thread.sleep(100);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }        
        
        TidePeak instant = new TidePeak(d, 0);
        SortedSet<TidePeak> after = cachedData.tailSet(instant);
        SortedSet<TidePeak> before = cachedData.headSet(instant);

        if (after.isEmpty() || before.isEmpty())
            return false;

        TidePeak immediatelyBefore = before.last();
        TidePeak immediatelyAfter = after.first();

        if ((instant.date.getTime() - immediatelyBefore.date.getTime()) > 1000 * 3600 * 12)
            return false;
        if ((immediatelyAfter.date.getTime() - instant.date.getTime()) > 1000 * 3600 * 12)
            return false;

        return true;
    }

    public void showSettings() {
        PluginUtils.editPluginProperties(this, true);
        try {
            PluginUtils.saveProperties("conf/conf/cachedData.properties", this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public Float getTidePrediction(Date date, boolean print) throws Exception {
        if (!contains(date))
            return null;
        else {
            TidePeak instant = new TidePeak(date, 0);
            TidePeak after = cachedData.tailSet(instant).first();
            TidePeak before = cachedData.headSet(instant).last();

            double totalTime = after.date.getTime() - before.date.getTime();
            double ellapsedTime = date.getTime() - before.date.getTime();

            double h1 = (before.height + after.height) / 2.0;
            double h2 = (before.height - after.height) / 2.0;
            double h3 = Math.cos((Math.PI * ellapsedTime) / totalTime);
            return (float) (h1 + h2 * h3);
        }
    }

    static class TidePeak implements Comparable<TidePeak> {

        public double height;
        public Date date;

        public TidePeak(Date date, double height) {
            this.date = date;
            this.height = height;
        }

        public TidePeak(int unixTimeSecs, double height) {
            this.date = new Date(unixTimeSecs * 1000);
            this.height = height;
        }

        @Override
        public int compareTo(TidePeak o) {
            return date.compareTo(o.date);
        }
        
        @Override
        public String toString() {
            return date+": "+height+" m";
        }
    }
    
    public void fetchData(String portName, Date aroundDate) throws Exception {
        try {
            Vector<TidePeak> newData = TideDataFetcher.fetchData(portName, aroundDate);
            System.out.println("Retrieved "+newData.size()+" new tide points.");
            update(newData);
            saveFile(portName, new File(GeneralPreferences.tidesFile.getParentFile(), portName+".txt"));            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws Exception {
        CachedData data = new CachedData();
        data.showSettings();
        data.fetchData("Leixoes", new Date());
        data.saveFile("Leixoes", new File("/home/zp/Desktop/leixoes.txt"));
        while (true) {
            Date d = new Date();
            System.out.println(d+": "+data.getTidePrediction(d, true));
            Thread.sleep(1000);
        }
    }
}
