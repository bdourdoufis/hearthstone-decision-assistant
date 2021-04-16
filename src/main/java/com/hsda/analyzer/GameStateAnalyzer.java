package com.hsda.analyzer;

import com.hsda.models.Card;
import com.hsda.models.GameState;
import com.hsda.service.CardFetcherService;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.Pattern;

public class GameStateAnalyzer {

    private GameState state;
    private Map<String, Integer> cardRankings;
    private Queue<String> messageQueue;

    public GameStateAnalyzer(GameState state) throws IOException, InterruptedException {
        this.state = state;
        messageQueue = new LinkedList<>();
        cardRankings = new HashMap<>();
        state.setAnalyzer(this);
        initializeCardRankings();
    }

    public void initializeCardRankings() throws IOException, InterruptedException {
        List<Card> cardsInDeck = new CardFetcherService().getDeckCards();
        for (Card c : cardsInDeck) {
            if (c.getName().equalsIgnoreCase("alleycat")
                    || c.getName().equalsIgnoreCase("wolpertinger")
                    || c.getName().equalsIgnoreCase("hecklefang hyena")
                    || c.getName().equalsIgnoreCase("bad luck albatross")) {
                cardRankings.put(c.getCardId(), 1);
            } else if (c.getName().equalsIgnoreCase("fiery bat")
                    || c.getName().equalsIgnoreCase("starving buzzard")
                    || c.getName().equalsIgnoreCase("master's call")) {
                cardRankings.put(c.getCardId(), 2);
            } else if (c.getName().equalsIgnoreCase("sunscale raptor")
                    || c.getName().equalsIgnoreCase("eaglehorn bow")
                    || c.getName().equalsIgnoreCase("kill command")) {
                cardRankings.put(c.getCardId(), 3);
            } else if (c.getName().equalsIgnoreCase("dire mole")
                    || c.getName().equalsIgnoreCase("scavenging hyena")) {
                cardRankings.put(c.getCardId(), 4);
            } else if (c.getName().equalsIgnoreCase("timber wolf")
                    || c.getName().equalsIgnoreCase("quick shot")) {
                cardRankings.put(c.getCardId(), 5);
            } else if (c.getName().equalsIgnoreCase("freezing trap")) {
                cardRankings.put(c.getCardId(), 6);
            }
        }
    }

    public void notifyMulliganBegins() {
        // The mulligan strategy for this deck is relatively simple:
        // Since our default gameplan is aggressive, we'll be looking to play on curve as much as possible.
        // This means we'll be looking to have at least a 1, 2, and 3 cost card in our opening hand.
        // In the case where we have multiple cards of the same cost in our opening hand, we'll reference the
        // preset card ranking list for our deck to determine what we should pitch.
        List<Card> openingHand = state.getFriendlyHand();
        List<Card> toMulligan = new ArrayList<>();
        Card oneCost = null;
        Card twoCost = null;
        Card threeCost = null;
        for (Card c : openingHand) {
            if (c.getCost() == 1) {
                if (oneCost != null) {
                    if (cardRankings.get(c.getCardId()) <= cardRankings.get(oneCost.getCardId())) {
                        toMulligan.add(oneCost);
                        oneCost = c;
                    } else {
                        toMulligan.add(c);
                    }
                } else {
                    oneCost = c;
                }
            } else if (c.getCost() == 2) {
                if (twoCost != null) {
                    if (cardRankings.get(c.getCardId()) <= cardRankings.get(twoCost.getCardId())) {
                        toMulligan.add(twoCost);
                        twoCost = c;
                    } else {
                        toMulligan.add(c);
                    }
                } else {
                    twoCost = c;
                }
            } else if (c.getCost() == 3) {
                if (threeCost != null) {
                    if (cardRankings.get(c.getCardId()) <= cardRankings.get(threeCost.getCardId())) {
                        toMulligan.add(threeCost);
                        threeCost = c;
                    } else {
                        toMulligan.add(c);
                    }
                } else {
                    threeCost = c;
                }
            } else if (c.getCost() != 0) {
                //If we have a card which costs more than three, we should mulligan it.
                toMulligan.add(c);
            }
        }

        if (toMulligan.isEmpty()) {
            messageQueue.add("Keep your entire opening hand.");
        } else {
            for (Card c : toMulligan) {
                messageQueue.add("Mulligan " + c.getName() + " away.");
            }
            messageQueue.add("Mulligan complete.");
        }
    }

    private boolean beastInHand(List<Card> hand) {
        for (Card c : hand) {
            if (c.getName().equalsIgnoreCase("alleycat")
                || c.getName().equalsIgnoreCase("tabbycat")
                || c.getName().equalsIgnoreCase("wolpertinger")
                || c.getName().equalsIgnoreCase("fiery bat")
                || c.getName().equalsIgnoreCase("hecklefang hyena")
                || c.getName().equalsIgnoreCase("starving buzzard")
                || c.getName().equalsIgnoreCase("sunscale raptor")
                || c.getName().equalsIgnoreCase("bad luck albatross")
                || c.getName().equalsIgnoreCase("dire mole")
                || c.getName().equalsIgnoreCase("sunscale raptor")
                || c.getName().equalsIgnoreCase("timber wolf")
                || c.getName().equalsIgnoreCase("scavenging hyena")
                || c.getName().equalsIgnoreCase("tundra rhino")) {
                return true;
            }
        }
       return false;
    }

    private boolean isBeast(Card c) {
        if (c.getName().equalsIgnoreCase("alleycat")
                || c.getName().equalsIgnoreCase("tabbycat")
                || c.getName().equalsIgnoreCase("wolpertinger")
                || c.getName().equalsIgnoreCase("fiery bat")
                || c.getName().equalsIgnoreCase("hecklefang hyena")
                || c.getName().equalsIgnoreCase("starving buzzard")
                || c.getName().equalsIgnoreCase("sunscale raptor")
                || c.getName().equalsIgnoreCase("bad luck albatross")
                || c.getName().equalsIgnoreCase("dire mole")
                || c.getName().equalsIgnoreCase("sunscale raptor")
                || c.getName().equalsIgnoreCase("timber wolf")
                || c.getName().equalsIgnoreCase("scavenging hyena")
                || c.getName().equalsIgnoreCase("tundra rhino")) {
            return true;
        }
       return false;
    }

    private int damageFromHandCalculation(List<Card> friendlyHand, int currentMana) {
        int damageFromHand = 0;
        int mana = currentMana;
        List<String> damageCards = new ArrayList<>();

        // First, we will determine which direct damage cards we have in hand.
        for (Card c : friendlyHand) {
            if (c.getName().equalsIgnoreCase("quick shot")) {
                damageCards.add("Quick Shot");
            } else if (c.getName().equalsIgnoreCase("kill command")) {
                damageCards.add("Kill Command");
            } else if (c.getName().equalsIgnoreCase("eaglehorn bow")) {
                damageCards.add("Eaglehorn Bow");
            } else if (c.getName().equalsIgnoreCase("tundra rhino")) {
                damageCards.add("Tundra Rhino");
            }
        }

        //Go through our damage sources from most efficient in terms of cost:damage ratio to least.
        while (damageCards.size() > 0) {
            if (damageCards.contains("Kill Command") && currentMana >= 3 && beastInHand(friendlyHand)) {
                damageFromHand += 5;
                damageCards.remove("Kill Command");
                currentMana -= 3;
            } else if (damageCards.contains("Quick Shot") && currentMana >= 2) {
                damageFromHand += 3;
                damageCards.remove("Quick Shot");
                currentMana -= 2;
            } else if (damageCards.contains("Eaglehorn Bow") && currentMana >= 3) {
                damageFromHand += 3;
                damageCards.remove("Eaglehorn Bow");
                currentMana -= 3;
            } else if (damageCards.contains("Kill Command") && currentMana >= 3 && !beastInHand(friendlyHand)) {
                damageFromHand += 3;
                damageCards.remove("Kill Command");
                currentMana -= 3;
            } else if (damageCards.contains("Tundra Rhino") && currentMana >= 5) {
                damageFromHand += 2;
                damageCards.remove("Tundra Rhino");
                currentMana -= 5;
            } else {
                //If none of the cases are met at this point, exit the loop.
                break;
            }
        }

        return damageFromHand;
    }

    private List<Card> getHighestDamageCombo(List<Card> friendlyHand, int currentMana) {
        List<Card> toReturn = new ArrayList<>();

        List<String> damageCards = new ArrayList<>();

        // First, we will determine which direct damage cards we have in hand.
        for (Card c : friendlyHand) {
            if (c.getName().equalsIgnoreCase("quick shot")) {
                damageCards.add("Quick Shot");
            } else if (c.getName().equalsIgnoreCase("kill command")) {
                damageCards.add("Kill Command");
            } else if (c.getName().equalsIgnoreCase("eaglehorn bow")) {
                damageCards.add("Eaglehorn Bow");
            } else if (c.getName().equalsIgnoreCase("tundra rhino")) {
                damageCards.add("Tundra Rhino");
            }
        }

        //Go through our damage sources from most efficient in terms of cost:damage ratio to least.
        while (damageCards.size() > 0) {
            if (damageCards.contains("Kill Command") && currentMana >= 3 && beastInHand(friendlyHand)) {
                damageCards.remove("Kill Command");
                currentMana -= 3;
                for (Card c : friendlyHand) {
                    if (c.getName().equalsIgnoreCase("Kill Command")) {
                        toReturn.add(c);
                    }
                }
            } else if (damageCards.contains("Quick Shot") && currentMana >= 2) {
                damageCards.remove("Quick Shot");
                currentMana -= 2;
                for (Card c : friendlyHand) {
                    if (c.getName().equalsIgnoreCase("Quick Shot")) {
                        toReturn.add(c);
                    }
                }
            } else if (damageCards.contains("Eaglehorn Bow") && currentMana >= 3) {
                damageCards.remove("Eaglehorn Bow");
                currentMana -= 3;
                for (Card c : friendlyHand) {
                    if (c.getName().equalsIgnoreCase("Eaglehorn Bow")) {
                        toReturn.add(c);
                    }
                }
            } else if (damageCards.contains("Kill Command") && currentMana >= 3 && !beastInHand(friendlyHand)) {
                damageCards.remove("Kill Command");
                currentMana -= 3;
                for (Card c : friendlyHand) {
                    if (c.getName().equalsIgnoreCase("Kill Command")) {
                        toReturn.add(c);
                    }
                }
            } else if (damageCards.contains("Tundra Rhino") && currentMana >= 5) {
                damageCards.remove("Tundra Rhino");
                currentMana -= 5;
                for (Card c : friendlyHand) {
                    if (c.getName().equalsIgnoreCase("Tundra Rhino")) {
                        toReturn.add(c);
                    }
                }
            } else {
                //If none of the cases are met at this point, exit the loop.
                break;
            }
        }

        return toReturn;
    }

    private int damageFromBoardCalculation(List<Card> friendlyBoard) {
        int damageCalc = 0;

        for (Card c : friendlyBoard) {
            damageCalc += c.getAttack();
        }

        return damageCalc;
    }

    private List<Card> killFromHand(List<Card> hand, Card enemyMinion, int currentMana) {
        List<Card> friendlyHand = new ArrayList<>(hand);
        int targetDamage = enemyMinion.getHealth();
        List<Card> toReturn = new ArrayList<>();

        //We're going to try to find the most efficient set of cards needed to kill the enemy minion.
        //Generally, this means we want to minimize the amount by which we overkill it.
        //We're still going to prioritize our most efficient cost/damage spells, but will spend more mana if necessary
        //to ensure a cleaner kill.
        int damageCommitted = 0;

        while (damageCommitted < targetDamage) {
            if (targetDamage < 3) {
                if (Card.listContainsCard("Tundra Rhino", friendlyHand) && currentMana >= 5) {
                    toReturn.add(Card.getCardFromListByName("Tundra Rhino", friendlyHand));
                    damageCommitted += 2;
                } else if (Card.listContainsCard("Quick Shot", friendlyHand) && currentMana >= 2) {
                    toReturn.add(Card.getCardFromListByName("Quick Shot", friendlyHand));
                    damageCommitted += 2;
                } else if (Card.listContainsCard("Kill Command", friendlyHand) && currentMana >= 3) {
                    toReturn.add(Card.getCardFromListByName("Kill Command", friendlyHand));
                    if (beastInHand(friendlyHand)) {
                        damageCommitted += 5;
                    } else {
                        damageCommitted += 3;
                    }
                }
            } else if (targetDamage == 3) {
                if (Card.listContainsCard("Quick Shot", friendlyHand) && currentMana >= 2) {
                    toReturn.add(Card.getCardFromListByName("Quick Shot", friendlyHand));
                    damageCommitted += 2;
                } else if (Card.listContainsCard("Kill Command", friendlyHand) && currentMana >= 3) {
                    toReturn.add(Card.getCardFromListByName("Kill Command", friendlyHand));
                    if (beastInHand(friendlyHand)) {
                        damageCommitted += 5;
                    } else {
                        damageCommitted += 3;
                    }
                }
            } else if (targetDamage <= 5 && targetDamage > 3) {
                if (Card.listContainsCard("Kill Command", friendlyHand)
                        && currentMana >= 3 && beastInHand(friendlyHand)) {
                    toReturn.add(Card.getCardFromListByName("Kill Command", friendlyHand));
                    damageCommitted += 5;
                } else if (Collections.frequency(friendlyHand, Card.getCardFromListByName("Quick Shot", friendlyHand)) == 2
                        && currentMana >= 4) {
                    toReturn.add(Card.getCardFromListByName("Quick Shot", friendlyHand));
                    damageCommitted += 6;
                } else if (Card.listContainsCard("Tundra Rhino", friendlyHand)
                        && Card.listContainsCard("Quick Shot", friendlyHand)
                        && currentMana >= 7) {
                    toReturn.add(Card.getCardFromListByName("Quick Shot", friendlyHand));
                    toReturn.add(Card.getCardFromListByName("Tundra Rhino", friendlyHand));
                    damageCommitted += 5;
                } else if (Card.listContainsCard("Tundra Rhino", friendlyHand)
                        && Card.listContainsCard("Kill Command", friendlyHand)
                        && currentMana >= 7) {
                    toReturn.add(Card.getCardFromListByName("Kill Command", friendlyHand));
                    toReturn.add(Card.getCardFromListByName("Tundra Rhino", friendlyHand));
                    //We don't need to check if there's a beast in hand- if there was the first case would have triggered.
                    damageCommitted += 5;
                } else if (Collections.frequency(friendlyHand, Card.getCardFromListByName("Quick Shot", friendlyHand)) == 2
                        && !beastInHand(friendlyHand) && currentMana >= 6) {
                    toReturn.add(Card.getCardFromListByName("Kill Command", friendlyHand));
                    toReturn.add(Card.getCardFromListByName("Kill Command", friendlyHand));
                    damageCommitted += 6;
                }
            } else if (targetDamage > 5) {
                //If there's no predetermined set of best combos to kill this minion (as its health is over 5)
                //we will instead iterate through our set of most efficient damage options, and select the best.
                if (Card.listContainsCard("Kill Command", friendlyHand)
                        && currentMana >= 3
                        && beastInHand(friendlyHand)) {
                    toReturn.add(Card.getCardFromListByName("Kill Command", friendlyHand));
                    friendlyHand.remove(Card.getCardFromListByName("Kill Command", friendlyHand));
                    currentMana -= 3;
                    damageCommitted += 5;
                } else if (Card.listContainsCard("Quick Shot", friendlyHand) && currentMana >= 2) {
                    toReturn.add(Card.getCardFromListByName("Quick Shot", friendlyHand));
                    friendlyHand.remove(Card.getCardFromListByName("Quick Shot", friendlyHand));
                    currentMana -= 2;
                    damageCommitted += 3;
                } else if (Card.listContainsCard("Eaglehorn Bow", friendlyHand) && currentMana >= 3) {
                    toReturn.add(Card.getCardFromListByName("Eaglehorn Bow", friendlyHand));
                    friendlyHand.remove(Card.getCardFromListByName("Eaglehorn Bow", friendlyHand));
                    currentMana -= 2;
                    damageCommitted += 3;
                } else if (Card.listContainsCard("Kill Command", friendlyHand)
                            && currentMana >= 3
                            && !beastInHand(friendlyHand)) {
                    toReturn.add(Card.getCardFromListByName("Kill Command", friendlyHand));
                    friendlyHand.remove(Card.getCardFromListByName("Kill Command", friendlyHand));
                    currentMana -= 3;
                    damageCommitted += 3;
                } else if (Card.listContainsCard("Tundra Rhino", friendlyHand)
                            && currentMana >= 5) {
                    toReturn.add(Card.getCardFromListByName("Tundra Rhino", friendlyHand));
                    friendlyHand.remove(Card.getCardFromListByName("Tundra Rhino", friendlyHand));
                    currentMana -= 5;
                    damageCommitted += 2;
                } else {
                    //If none of the cases are met at this point, exit the loop.
                    break;
                }
            }
        }

        return toReturn;
    }

    private boolean canValueTrade(Card friendlyMinion, List<Card> enemyBoard) {
        for (Card c : enemyBoard) {
            if ((c.getHealth() <= friendlyMinion.getAttack()
                && friendlyMinion.getHealth() > c.getAttack())) {
                return true;
            }
        }
        return false;
    }

    private Card getValueTradeTarget(Card friendlyMinion, List<Card> enemyBoard) {
        //We'll direct our value trade into the most expensive minion possible.
        //This ensures that the play is as high-tempo as possible.
        Card highestCost = null;
        for (Card c : enemyBoard) {
            if ((c.getHealth() <= friendlyMinion.getAttack()
                    && friendlyMinion.getHealth() > c.getAttack())) {
                if (highestCost == null) {
                    highestCost = c;
                } else if (c.getCost() > highestCost.getCost()) {
                    highestCost = c;
                }
            }
        }
        return highestCost;
    }

    //This method informs the analyzer that it should pull the current game state and generate messages
    public void notifyTurnBegins() {
        //TODO: Add special cases for when the player has The Coin
        List<Card> friendlyHand = new ArrayList<>(state.getFriendlyHand());
        List<Card> friendlyBoard = new ArrayList<>(state.getFriendlyBoard());
        List<Card> enemyBoard = new ArrayList<>(state.getEnemyBoard());
        int enemyHandSize = state.getEnemyHandSize();
        int currentManaCount = state.getMana();

        // The number of minions on our board. This may fluctuate throughout the turn, so we'll keep track of it here.
        int boardSize = friendlyBoard.size();

        // The amount of damage we can produce from our hand.
        int damageFromHand = damageFromHandCalculation(friendlyHand, currentManaCount);

        // The amount of damage we can produce from our board.
        int damageFromBoard = damageFromBoardCalculation(friendlyBoard);

        // Indicates if we have a Tundra Rhino in play, which allows our beasts to attack the turn we play them.
        boolean rhinoInPlay = Card.listContainsCard("Tundra Rhino", friendlyBoard);

        // Indicates if we have a timber wolf in play, which buffs the attack of our other basts by one.
        boolean timberWolfInPlay = Card.listContainsCard("Timber Wolf", friendlyBoard);

        if (enemyBoard.size() == 0) {
            //If the opponent has no cards in play, we want to play out the highest cost minions from our hand.
            //We also want to send all of our attackers at the opponent's face.
            for (Card c : friendlyBoard) {
                messageQueue.add("Attack the opponent's face with " + c.getName() + ".");
                state.damageOpponent(c.getAttack());
            }
            while (currentManaCount > 0) {
                Card toPlay = null;
                int highestCost = 0;
                for (Card c : friendlyHand) {
                    if (c.getCost() > highestCost
                            && c.getCost() <= currentManaCount
                            && c.getType().equalsIgnoreCase("minion")) {
                        highestCost = c.getCost();
                    }
                }

                if (highestCost == 0) {
                    if (currentManaCount >= 2 && damageFromHand > 0) {
                        List<Card> directDamageCards = getHighestDamageCombo(friendlyHand, currentManaCount);
                        state.damageOpponent(damageFromHandCalculation(friendlyHand, currentManaCount));
                        for (Card c : directDamageCards) {
                            messageQueue.add("Use " + c.getName() + " on the opponent's face.");
                            friendlyHand.remove(c);
                            currentManaCount -= c.getCost();
                        }
                    }
                    break;
                } else if (highestCost == 1 && currentManaCount == 2) {
                    messageQueue.add("Use your hero power.");
                    currentManaCount -= 2;
                } else {
                    if (friendlyHand.isEmpty()) {
                        messageQueue.add("Use your hero power.");
                        currentManaCount -= 2;
                        break;
                    }
                    for (Card c : friendlyHand) {
                        if (toPlay == null) {
                            if (c.getCost() == highestCost) {
                                toPlay = c;
                            }
                        } else if (c.getCost() == highestCost
                                && cardRankings.get(c.getCardId()) < cardRankings.get(toPlay.getCardId())) {
                            toPlay = c;
                        }
                    }
                    if (toPlay != null && boardSize < 7) {
                        messageQueue.add("Play " + toPlay.getName() + ".");
                        friendlyHand.remove(toPlay);
                        currentManaCount -= toPlay.getCost();
                        boardSize++;

                        if (toPlay.getName().equalsIgnoreCase("Tundra Rhino")) {
                            rhinoInPlay = true;
                        } else if (toPlay.getName().equalsIgnoreCase("Timber Wolf")) {
                            timberWolfInPlay = true;
                        }

                        if (rhinoInPlay) {
                            messageQueue.add("Attack the opponent's face with " + toPlay.getName() + ".");
                        }
                    } else if (boardSize == 7 && toPlay.getCost() > 1) {
                        messageQueue.add("Use your hero power.");
                        currentManaCount -= 2;
                    }
                }
            }
        } else {
            //If the opponent has minions in play, we'll need to do a few things.
            //Firstly, we must check if any of their minions have taunt. If so, we need to remove them
            //if possible.
            List<Card> enemyTauntMinions = new ArrayList<>();
            for (Card c : enemyBoard) {
                if (c.getMechanics().contains("Taunt")) {
                    enemyTauntMinions.add(c);
                }
            }

            if (enemyTauntMinions.size() > 0) {
                //When removing the opponent's taunt minions, we would prefer to utilize direct damage from our
                //hand over our minions when possible, as maintaining control of the board is always our highest
                //priority.
                int totalTauntHealth = 0;
                for (Card c : enemyBoard) {
                    if (c.getMechanics().contains("Taunt")) {
                        totalTauntHealth += c.getHealth();
                    }
                }

                while (damageFromHand >= totalTauntHealth) {
                    //Get the combination of cards needed to kill the taunt and tell the player to play them
                    //We want to prioritize killing the highest health taunts first.
                    Card highestHp = null;
                    for (Card c : enemyTauntMinions) {
                        if (highestHp == null) {
                            highestHp = c;
                        } else if (c.getHealth() > highestHp.getHealth()) {
                            highestHp = c;
                        }
                    }

                    List<Card> cardsToKill = killFromHand(friendlyHand, highestHp, currentManaCount);
                    for (Card d : cardsToKill) {
                        messageQueue.add("Use " + d.getName() + " on the opponent's " + highestHp.getName() + ".");
                        friendlyHand.remove(d);
                        currentManaCount -= d.getCost();
                    }
                    //Update the amount of damage we have available from our hand.
                    damageFromHand = damageFromHandCalculation(friendlyHand, currentManaCount);
                    totalTauntHealth -= highestHp.getHealth();
                }

                if (damageFromHand + damageFromBoard >= totalTauntHealth) {
                    //Use all of our hand damage and the most efficient possible trades to kill the taunt
                } else {
                    //If we can't remove the taunt, see if we can trade damage into it without losing minions. If so,
                    //do that. We'll also want to send any damage from our hand upstairs and hero power if possible.
                }
            } else {
                //If the opponent has no taunt minions in play, we should see if we can get any value trades.
                //We really only want to trade to maintain our board presence.
                //We will also want to keep track of which of our minions have attacked, so we can send the rest face.
                List<Card> alreadyAttacked = new ArrayList<>();
                for (Card c : friendlyBoard) {
                    if (canValueTrade(c, enemyBoard)) {
                        messageQueue.add("Attack the opponent's "
                                            + getValueTradeTarget(c, enemyBoard).getName()
                                            + " with your " + c.getName());
                        enemyBoard.remove(getValueTradeTarget(c, enemyBoard));
                        alreadyAttacked.add(c);
                    } else {
                        messageQueue.add("Attack the opponent's face with " + c.getName() + ".");
                    }
                }
                //After this, we'll proceed as normal, playing out the best, highest-cost minions from our hand.
                while (currentManaCount > 0) {
                    Card toPlay = null;
                    int highestCost = 0;
                    for (Card c : friendlyHand) {
                        if (c.getCost() > highestCost
                                && c.getCost() <= currentManaCount) {
                            highestCost = c.getCost();
                        }
                    }

                    if (highestCost == 0) {
                        if (currentManaCount >= 2 && damageFromHand > 0) {
                            List<Card> directDamageCards = getHighestDamageCombo(friendlyHand, currentManaCount);
                            state.damageOpponent(damageFromHandCalculation(friendlyHand, currentManaCount));
                            for (Card c : directDamageCards) {
                                if (c.getName().equalsIgnoreCase("eaglehorn bow")) {
                                    //TODO: Get bow information for special case, when bow goes to gy unequip it
                                    messageQueue.add("Equip " + c.getName() + ".");
                                    messageQueue.add("Attack the opponent's face with your " + c.getName() + ".");
                                } else {
                                    messageQueue.add("Use " + c.getName() + " on the opponent's face.");
                                }
                                friendlyHand.remove(c);
                                currentManaCount -= c.getCost();
                            }
                        } else if (currentManaCount >= 2) {
                            messageQueue.add("Use your hero power.");
                            currentManaCount -= 2;
                        }
                        break;
                    } else if (highestCost == 1 && currentManaCount == 2) {
                        messageQueue.add("Use your hero power.");
                        currentManaCount -= 2;
                    } else {
                        if (friendlyHand.isEmpty()) {
                            messageQueue.add("Use your hero power.");
                            currentManaCount -= 2;
                            break;
                        }
                        for (Card c : friendlyHand) {
                            if (toPlay == null) {
                                if (c.getCost() == highestCost) {
                                    toPlay = c;
                                }
                            } else if (c.getCost() == highestCost
                                    && cardRankings.get(c.getCardId()) < cardRankings.get(toPlay.getCardId())) {
                                toPlay = c;
                            }
                        }
                        if (toPlay != null && boardSize < 7) {
                            messageQueue.add("Play " + toPlay.getName() + ".");
                            friendlyHand.remove(toPlay);
                            currentManaCount -= toPlay.getCost();
                            boardSize++;

                            if (toPlay.getName().equalsIgnoreCase("Tundra Rhino")) {
                                rhinoInPlay = true;
                            }

                            if (rhinoInPlay) {
                                messageQueue.add("Attack the opponent's face with " + toPlay.getName() + ".");
                            }
                        } else if (boardSize == 7 && toPlay.getType().equalsIgnoreCase("minion")) {
                            messageQueue.add("Use your hero power.");
                            currentManaCount -= 2;
                        }
                    }
                }
            }
        }

        // Obviously, this is the last thing we will do each turn.
        messageQueue.add("Pass the turn.");
    }

    public void notifyTurnEnds() {

    }

    public boolean hasMessage() {
        return messageQueue.peek() != null;
    }

    public String getMessage() {
        // This should only be called after the hasMessage() method.
        // As such, we shouldn't need to validate that a message is actually present.
        return messageQueue.remove();
    }
}