/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.campaign.personnel;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.options.IOption;
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;

/**
 * Parses custom SPA file and passes data to the PersonnelOption constructor so
 * the custom
 * abilities are included.
 *
 * @author Neoancient
 */
public class CustomOption {
    private static final MMLogger logger = MMLogger.create(CustomOption.class);

    private static List<CustomOption> customAbilities = null;

    private String name;
    private String group;
    private int type;
    private Object defaultVal;

    private CustomOption(String key) {
        this.name = key;
        group = PersonnelOptions.LVL3_ADVANTAGES;
        type = IOption.BOOLEAN;
        defaultVal = Boolean.FALSE;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public int getType() {
        return type;
    }

    public Object getDefault() {
        return defaultVal;
    }

    /**
     * Loads custom abilities from the data directory the first time it is called.
     *
     * @return The list of user-defined special abilities.
     */
    public static List<CustomOption> getCustomAbilities() {
        if (null == customAbilities) {
            initCustomAbilities();
        }
        return customAbilities;
    }

    private static void initCustomAbilities() {
        customAbilities = new ArrayList<>();

        Document xmlDoc;

        try (InputStream is = new FileInputStream("data/universe/customspa.xml")) { // TODO : Remove inline file path
            // Using factory get an instance of document builder
            DocumentBuilder db = MHQXMLUtility.newUnsafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(is);
        } catch (Exception ex) {
            logger.error("", ex);
            return;
        }

        Element spaEle = xmlDoc.getDocumentElement();
        NodeList nl = spaEle.getChildNodes();

        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML. At least this cleans it up.
        spaEle.normalize();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            if (!wn.getParentNode().equals(spaEle)) {
                continue;
            }

            int xc = wn.getNodeType();

            if (xc == Node.ELEMENT_NODE) {
                // This is what we really care about.
                // All the meat of our document is in this node type, at this
                // level.
                // Okay, so what element is it?
                String xn = wn.getNodeName();

                if (xn.equalsIgnoreCase("option")) {
                    CustomOption option = CustomOption.generateInstanceFromXML(wn);
                    if (null != option) {
                        customAbilities.add(option);
                    }
                }
            }
        }
    }

    public static CustomOption generateInstanceFromXML(Node wn) {
        String key = wn.getAttributes().getNamedItem("name").getTextContent();
        if (null == key) {
            logger.error("Custom ability does not have a 'name' attribute.");
            return null;
        }

        CustomOption retVal = new CustomOption(key);
        try {
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("group")) {
                    retVal.group = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("type")) {
                    retVal.type = Integer.parseInt(wn2.getTextContent());
                }
            }

            switch (retVal.type) {
                case IOption.BOOLEAN:
                    retVal.defaultVal = Boolean.FALSE;
                    break;
                case IOption.INTEGER:
                    retVal.defaultVal = 0;
                    break;
                case IOption.FLOAT:
                    retVal.defaultVal = 0.0f;
                    break;
                case IOption.STRING:
                case IOption.CHOICE:
                default:
                    retVal.defaultVal = "";
                    break;
            }
        } catch (Exception ex) {
            logger.error("Error parsing custom ability " + retVal.name, ex);
        }

        return retVal;
    }
}
