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
 * 24 May 2016
 */

package pt.lsts.neptus.plugins.mvplanning.tests;

import java.util.List;

import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapCell;
import pt.lsts.neptus.plugins.mvplanning.planning.algorithm.Astar;
import pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition.GridArea;
import pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition.GridCell;
import pt.lsts.neptus.types.coord.LocationType;

public class TestAstar {
    public static void main(String[] args) {
        GridArea area = new GridArea(5, 25, 25, 0, LocationType.FEUP);
        GridCell[][] cells = area.getAllCells();
        GridCell startCell = cells[0][0];
        GridCell endCell = cells[4][4];

        Astar astar = new Astar();
        List<ManeuverLocation> path = astar.computeShortestPath(startCell, endCell);

        for(ManeuverLocation cellLoc : path) {
            MapCell cell = area.getClosestCell(cellLoc);
            System.out.println("* " + cell.id());
        }
    }
}
