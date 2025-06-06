/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.unitMarket;

import static mekhq.MHQConstants.BATTLE_OF_TUKAYYID;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import megamek.Version;
import megamek.client.ratgenerator.MissionRole;
import megamek.common.Compute;
import megamek.common.EntityMovementMode;
import megamek.common.MekSummary;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.market.enums.UnitMarketType;
import mekhq.campaign.universe.Faction;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class AbstractUnitMarket {
    private static final MMLogger logger = MMLogger.create(AbstractUnitMarket.class);

    // region Variable Declarations
    private final UnitMarketMethod method;
    private List<UnitMarketOffer> offers;

    protected final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Constructors
    protected AbstractUnitMarket(final UnitMarketMethod method) {
        this.method = method;
        setOffers(new ArrayList<>());
    }
    // endregion Constructors

    // region Getters/Setters
    public UnitMarketMethod getMethod() {
        return method;
    }

    public List<UnitMarketOffer> getOffers() {
        return offers;
    }

    public void setOffers(final List<UnitMarketOffer> offers) {
        this.offers = offers;
    }
    // endregion Getters/Setters

    // region Process New Day

    /**
     * This is the primary method for processing the Unit Market. It is executed as part of {@link Campaign#newDay()}
     *
     * @param campaign the campaign to process the Unit Market new day using
     */
    public abstract void processNewDay(Campaign campaign);

    // region Generate Offers

    /**
     * This is the primary Unit Market generation method, which is how the market specified generates unit offers
     *
     * @param campaign the campaign to generate the unit offers for
     */
    public abstract void generateUnitOffers(Campaign campaign);

    /**
     * This adds a number of unit offers
     *
     * @param campaign    the campaign to add the offers based on
     * @param number      the number of units to generate
     * @param market      the unit market type the unit is part of
     * @param unitType    the unit type to generate
     * @param faction     the faction to add the offers for, or null. If null, that must be handled within this method
     *                    before any generated offers may be added to the market.
     * @param quality     the quality of the unit to generate
     * @param priceTarget the target number used to determine the percent
     */
    public abstract void addOffers(Campaign campaign, int number, UnitMarketType market, int unitType,
          @Nullable Faction faction, int quality, int priceTarget);

    /**
     * @param campaign the campaign to use to generate the unit
     * @param market   the market type the unit is being offered in
     * @param unitType the unit type to generate the unit with
     * @param faction  the faction to generate the unit from
     * @param quality  the quality to generate the unit at
     * @param percent  the percentage of the original unit cost the unit will be offered at
     *
     * @return the name of the unit that has been added to the market, or null if none were added
     */
    public @Nullable String addSingleUnit(final Campaign campaign, final UnitMarketType market, final int unitType,
          final Faction faction, final int quality, final int percent) {
        return addSingleUnit(campaign,
              market,
              unitType,
              faction,
              quality,
              new ArrayList<>(),
              new ArrayList<>(),
              percent);
    }

    /**
     * @param campaign      the campaign to use to generate the unit offer
     * @param market        the market type the unit is being offered in
     * @param unitType      the unit type to generate the unit with
     * @param faction       the faction to generate the unit from
     * @param quality       the quality to generate the unit at
     * @param movementModes the movement modes to generate for
     * @param missionRoles  the mission roles to generate for
     * @param percent       the percentage of the original unit cost the unit will be offered at
     *
     * @return the name of the unit that has been added to the market, or null if none were added
     */
    public @Nullable String addSingleUnit(final Campaign campaign, final UnitMarketType market, final int unitType,
          final Faction faction, final int quality, final Collection<EntityMovementMode> movementModes,
          final Collection<MissionRole> missionRoles, final int percent) {
        return addSingleUnit(campaign,
              market,
              unitType,
              faction,
              generateWeight(campaign, unitType, faction),
              quality,
              movementModes,
              missionRoles,
              percent);
    }

    /**
     * @param campaign      the campaign to use to generate the unit offer
     * @param market        the market type the unit is being offered in
     * @param unitType      the unit type to generate the unit with
     * @param faction       the faction to generate the unit from
     * @param weight        the weight class to generate the unit at
     * @param quality       the quality to generate the unit at
     * @param movementModes the movement modes to generate for
     * @param missionRoles  the mission roles to generate for
     * @param percent       the percentage of the original unit cost the unit will be offered at
     *
     * @return the name of the unit that has been added to the market, or null if none were added
     */
    protected @Nullable String addSingleUnit(final Campaign campaign, final UnitMarketType market, final int unitType,
          final Faction faction, final int weight, final int quality,
          final Collection<EntityMovementMode> movementModes, final Collection<MissionRole> missionRoles,
          final int percent) {
        final MekSummary mekSummary = campaign.getUnitGenerator()
                                            .generate(faction.getShortName(),
                                                  unitType,
                                                  weight,
                                                  campaign.getGameYear(),
                                                  quality,
                                                  movementModes,
                                                  missionRoles,
                                                  ms -> (!campaign.getCampaignOptions().isLimitByYear() ||
                                                               (campaign.getGameYear() > ms.getYear())) &&
                                                              (!ms.isClan() ||
                                                                     campaign.getCampaignOptions()
                                                                           .isAllowClanPurchases()) &&
                                                              (ms.isClan() ||
                                                                     campaign.getCampaignOptions()
                                                                           .isAllowISPurchases()));
        return (mekSummary == null) ? null : addSingleUnit(campaign, market, unitType, mekSummary, percent);
    }

    /**
     * @param campaign   the campaign to use to generate the offer
     * @param market     the market type the unit is being offered in
     * @param unitType   the unit type of the generated unit
     * @param mekSummary the generated mek summary
     * @param percent    the percentage of the original unit cost the unit will be offered at
     *
     * @return the name of the unit that has been added to the market
     */
    public String addSingleUnit(final Campaign campaign, final UnitMarketType market, final int unitType,
          final MekSummary mekSummary, final int percent) {

        Faction campaignFaction = campaign.getFaction();
        LocalDate currentDate = campaign.getLocalDate();

        if (!campaignFaction.isClan()) {
            if (mekSummary.isClan() && currentDate.isBefore(BATTLE_OF_TUKAYYID)) {
                return null;
            }
        }

        getOffers().add(new UnitMarketOffer(market, unitType, mekSummary, percent, generateTransitDuration(campaign)));

        return mekSummary.getName();
    }

    /**
     * @param campaign the campaign to generate the unit weight based on
     * @param unitType the unit type to determine the format of weight to generate
     * @param faction  the faction to generate the weight for
     *
     * @return the generated weight
     */
    protected abstract int generateWeight(Campaign campaign, int unitType, Faction faction);

    /**
     * @param campaign the campaign to use to generate the transit duration
     *
     * @return the generated transit duration
     */
    protected int generateTransitDuration(final Campaign campaign) {
        if (campaign.getCampaignOptions().isInstantUnitMarketDelivery()) {
            return 0;
        } 
        return campaign.calculatePartTransitTime(Compute.d6(2) - 2);
    }

    /**
     * @param campaign the campaign to write the refresh report to
     */
    protected void writeRefreshReport(final Campaign campaign) {
        if (campaign.getCampaignOptions().isUnitMarketReportRefresh()) {
            campaign.addReport(resources.getString("AbstractUnitMarket.RefreshReport.report"));
        }
    }
    // endregion Generate Offers

    // region Offer Removal

    /**
     * This is the primary Unit Market removal method, which is how the market specified removes unit offers
     *
     * @param campaign the campaign to use in determining the offers to remove
     */
    protected abstract void removeUnitOffers(Campaign campaign);
    // endregion Offer Removal
    // endregion Process New Day

    // region File I/O

    /**
     * This writes the Unit Market to XML
     *
     * @param pw     the PrintWriter to write to
     * @param indent the base indent level to write at
     */
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "unitMarket");
        writeBodyToXML(pw, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "unitMarket");
    }

    /**
     * This is meant to be overridden so that a market can have additional elements added to it, albeit with this called
     * by super.writeBodyToXML(pw, indent) first.
     *
     * @param pw     the PrintWriter to write to
     * @param indent the base indent level to write at
     */
    protected void writeBodyToXML(final PrintWriter pw, int indent) {
        for (final UnitMarketOffer offer : getOffers()) {
            offer.writeToXML(pw, indent);
        }
    }

    /**
     * This method fills the market based on the supplied XML node. The market is initialized as empty before this is
     * called.
     *
     * @param wn       the node to fill the market from
     * @param campaign the campaign the market is being parsed as part of
     * @param version  the version of the market being parsed
     */
    public void fillFromXML(final Node wn, final Campaign campaign, final Version version) {
        try {
            final NodeList nl = wn.getChildNodes();
            for (int x = 0; x < nl.getLength(); x++) {
                final Node wn2 = nl.item(x);
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                parseXMLNode(wn2, campaign, version);
            }
        } catch (Exception ex) {
            logger.error("Failed to parse Unit Market, keeping currently parsed market", ex);
        }
    }

    /**
     * This is meant to be overridden so that a market can have additional elements added to it, albeit with this called
     * by super.parseXMLNode(wn) first.
     *
     * @param wn       the node to parse from XML
     * @param campaign the campaign the market is being parsed as part of
     * @param version  the version of the market being parsed
     */
    protected void parseXMLNode(final Node wn, final Campaign campaign, final Version version) {
        if (wn.getNodeName().equalsIgnoreCase("offer")) {
            final UnitMarketOffer offer = UnitMarketOffer.generateInstanceFromXML(wn, campaign, version);
            if (offer != null) {
                getOffers().add(offer);
            }
        }
    }
    // endregion File I/O
}
