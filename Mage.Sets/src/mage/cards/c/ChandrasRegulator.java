package mage.cards.c;

import mage.ObjectColor;
import mage.abilities.Ability;
import mage.abilities.LoyaltyAbility;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.costs.common.DiscardTargetCost;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.costs.mana.GenericManaCost;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.Effect;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.DrawCardSourceControllerEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.*;
import mage.filter.FilterCard;
import mage.filter.predicate.Predicates;
import mage.filter.predicate.mageobject.ColorPredicate;
import mage.filter.predicate.mageobject.SubtypePredicate;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.game.stack.StackAbility;
import mage.players.Player;
import mage.target.common.TargetCardInHand;

import java.util.UUID;

/**
 * @author TheElk801
 */
public final class ChandrasRegulator extends CardImpl {

    private static final FilterCard filter = new FilterCard("a Mountain card or a red card");

    static {
        filter.add(Predicates.or(
                new SubtypePredicate(SubType.MOUNTAIN),
                new ColorPredicate(ObjectColor.RED)
        ));
    }

    public ChandrasRegulator(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT}, "{1}{R}");

        this.addSuperType(SuperType.LEGENDARY);

        // Whenever you activate a loyalty ability of a Chandra planeswalker, you may pay {1}. If you do, copy that ability. You may choose new targets for the copy.
        this.addAbility(new ChandrasRegulatorTriggeredAbility());

        // {1}, {T}, Discard a Mountain card or a red card: Draw a card.
        Ability ability = new SimpleActivatedAbility(
                new DrawCardSourceControllerEffect(1), new GenericManaCost(1)
        );
        ability.addCost(new TapSourceCost());
        ability.addCost(new DiscardTargetCost(new TargetCardInHand(filter)));
        this.addAbility(ability);
    }

    private ChandrasRegulator(final ChandrasRegulator card) {
        super(card);
    }

    @Override
    public ChandrasRegulator copy() {
        return new ChandrasRegulator(this);
    }
}

class ChandrasRegulatorTriggeredAbility extends TriggeredAbilityImpl {

    ChandrasRegulatorTriggeredAbility() {
        super(Zone.BATTLEFIELD, new ChandrasRegulatorEffect(), false);
    }

    private ChandrasRegulatorTriggeredAbility(final ChandrasRegulatorTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public ChandrasRegulatorTriggeredAbility copy() {
        return new ChandrasRegulatorTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.ACTIVATED_ABILITY;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (!event.getPlayerId().equals(getControllerId())) {
            return false;
        }
        StackAbility stackAbility = (StackAbility) game.getStack().getStackObject(event.getSourceId());
        if (stackAbility == null || !(stackAbility.getStackAbility() instanceof LoyaltyAbility)) {
            return false;
        }
        Permanent permanent = stackAbility.getSourcePermanentOrLKI(game);
        if (permanent == null || !permanent.isPlaneswalker()
                || !permanent.hasSubtype(SubType.CHANDRA, game)) {
            return false;
        }
        Effect effect = this.getEffects().get(0);
        effect.setValue("stackAbility", stackAbility);
        return true;
    }

    @Override
    public String getRule() {
        return "Whenever you activate a loyalty ability of a Chandra planeswalker, you may pay {1}. " +
                "If you do, copy that ability. You may choose new targets for the copy.";
    }
}

class ChandrasRegulatorEffect extends OneShotEffect {

    ChandrasRegulatorEffect() {
        super(Outcome.Benefit);
    }

    private ChandrasRegulatorEffect(final ChandrasRegulatorEffect effect) {
        super(effect);
    }

    @Override
    public ChandrasRegulatorEffect copy() {
        return new ChandrasRegulatorEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(source.getControllerId());
        ManaCostsImpl cost = new ManaCostsImpl("{1}");
        if (player == null) {
            return false;
        }
        if (!cost.canPay(source, source.getSourceId(), player.getId(), game)
                || !player.chooseUse(Outcome.Benefit, "Pay " + cost.getText() +
                "? If you do, copy that ability. You may choose new targets for the copy.", source, game)) {
            return true;
        }
        if (!cost.pay(source, game, source.getSourceId(), source.getControllerId(), false, null)) {
            return true;
        }
        StackAbility ability = (StackAbility) getValue("stackAbility");
        Player controller = game.getPlayer(source.getControllerId());
        Permanent sourcePermanent = game.getPermanentOrLKIBattlefield(source.getSourceId());
        if (ability == null || controller == null || sourcePermanent == null) {
            return false;
        }
        ability.createCopyOnStack(game, source, source.getControllerId(), true);
        game.informPlayers(sourcePermanent.getIdName() + ": " + controller.getLogName() + " copied activated ability");
        return true;
    }
}
