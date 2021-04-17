package com.hsda.analyzer;

import com.hsda.models.Card;
import com.hsda.models.GameState;
import com.hsda.service.CardFetcherService;

import java.io.IOException;
import java.util.*;

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
                    || c.getName().equalsIgnoreCase("bad luck albatross")
                    || c.getName().equalsIgnoreCase("tundra rhino")) {
                cardRankings.put(c.getCardId(), 1);
            } else if (c.getName().equalsIgnoreCase("fiery bat")
                    || c.getName().equalsIgnoreCase("master's call")) {
                cardRankings.put(c.getCardId(), 2);
            } else if (c.getName().equalsIgnoreCase("sunscale raptor")
                    || c.getName().equalsIgnoreCase("eaglehorn bow")
                    || c.getName().equalsIgnoreCase("quick shot")
                    || c.getName().equalsIgnoreCase("kill command")) {
                cardRankings.put(c.getCardId(), 3);
            } else if (c.getName().equalsIgnoreCase("dire mole")) {
                cardRankings.put(c.getCardId(), 4);
            } else if (c.getName().equalsIgnoreCase("timber wolf")
                    || c.getName().equalsIgnoreCase("scavenging hyena")) {
                cardRankings.put(c.getCardId(), 5);
            } else if (c.getName().equalsIgnoreCase("freezing trap")) {
                cardRankings.put(c.getCardId(), 6);
            } else if (c.getName().equalsIgnoreCase("starving buzzard")) {
                // We want to set the priority of this card very low as we don't actually want to play it on curve.
                // There will be a special case for 'buzzard turns'
                cardRankings.put(c.getCardId(), 10);
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

    private boolean beastInHand(List<Card> hand) {
        for (Card c : hand) {
            if (isBeast(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean beastInPlay(List<Card> friendlyBoard) {
        for (Card c : friendlyBoard) {
            if (isBeast(c)) {
                return true;
            }
        }
        return false;
    }

    private int damageFromHandCalculation(List<Card> friendlyHand, int currentMana,  List<Card> friendlyBoard) {
        int damageFromHand = 0;
        int mana = currentMana;
        List<String> damageCards = new ArrayList<>();

        // First, we will determine which direct damage cards we have in hand.
        for (Card c : friendlyHand) {
            if (c.getName().equalsIgnoreCase("quick shot")) {
                damageCards.add("Quick Shot");
            } else if (c.getName().equalsIgnoreCase("kill command")) {
                damageCards.add("Kill Command");
            } /*else if (c.getName().equalsIgnoreCase("eaglehorn bow")) {
                damageCards.add("Eaglehorn Bow");
            }*/ else if (c.getName().equalsIgnoreCase("tundra rhino")) {
                damageCards.add("Tundra Rhino");
            }
        }

        //Go through our damage sources from most efficient in terms of cost:damage ratio to least.
        while (damageCards.size() > 0) {
            if (damageCards.contains("Kill Command") && currentMana >= 3 && beastInPlay(friendlyBoard)) {
                damageFromHand += 5;
                damageCards.remove("Kill Command");
                currentMana -= 3;
            } else if (damageCards.contains("Quick Shot") && currentMana >= 2) {
                damageFromHand += 3;
                damageCards.remove("Quick Shot");
                currentMana -= 2;
            } /*else if (damageCards.contains("Eaglehorn Bow") && currentMana >= 3) {
                damageFromHand += 3;
                damageCards.remove("Eaglehorn Bow");
                currentMana -= 3;
            }*/ else if (damageCards.contains("Kill Command") && currentMana >= 3 && !beastInPlay(friendlyBoard)) {
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

    private List<Card> getHighestDamageCombo(List<Card> friendlyHand, int currentMana,  List<Card> friendlyBoard) {
        List<Card> toReturn = new ArrayList<>();

        List<String> damageCards = new ArrayList<>();

        // First, we will determine which direct damage cards we have in hand.
        for (Card c : friendlyHand) {
            if (c.getName().equalsIgnoreCase("quick shot")) {
                damageCards.add("Quick Shot");
            } else if (c.getName().equalsIgnoreCase("kill command")) {
                damageCards.add("Kill Command");
            } /*else if (c.getName().equalsIgnoreCase("eaglehorn bow")) {
                damageCards.add("Eaglehorn Bow");
            }*/ else if (c.getName().equalsIgnoreCase("tundra rhino")) {
                damageCards.add("Tundra Rhino");
            }
        }

        //Go through our damage sources from most efficient in terms of cost:damage ratio to least.
        while (damageCards.size() > 0) {
            if (damageCards.contains("Kill Command") && currentMana >= 3 && beastInPlay(friendlyBoard)) {
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
            } /*else if (damageCards.contains("Eaglehorn Bow") && currentMana >= 3) {
                damageCards.remove("Eaglehorn Bow");
                currentMana -= 3;
                for (Card c : friendlyHand) {
                    if (c.getName().equalsIgnoreCase("Eaglehorn Bow")) {
                        toReturn.add(c);
                    }
                }
            }*/ else if (damageCards.contains("Kill Command") && currentMana >= 3 && !beastInPlay(friendlyBoard)) {
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

        if (state.isBowEquipped()) {
            damageCalc += 3;
        }

        return damageCalc;
    }

    private List<Card> killFromHand(List<Card> hand, Card enemyMinion, int currentMana, List<Card> friendlyBoard) {
        List<Card> friendlyHand = new ArrayList<>(hand);
        int targetDamage = enemyMinion.getHealth();
        List<Card> toReturn = new ArrayList<>();

        //We're going to try to find the most efficient set of cards needed to kill the enemy minion.
        //Generally, this means we want to minimize the amount by which we overkill it.
        //We're still going to prioritize our most efficient cost/damage spells, but will spend more mana if necessary
        //to ensure a cleaner kill.
        int damageCommitted = 0;

        while (damageCommitted < targetDamage && currentMana >= 2) {
            if (targetDamage < 3) {
                if (Card.listContainsCard("Tundra Rhino", friendlyHand) && currentMana >= 5) {
                    toReturn.add(Card.getCardFromListByName("Tundra Rhino", friendlyHand));
                    currentMana -= 5;
                    damageCommitted += 2;
                } else if (Card.listContainsCard("Quick Shot", friendlyHand) && currentMana >= 2) {
                    toReturn.add(Card.getCardFromListByName("Quick Shot", friendlyHand));
                    currentMana -= 2;
                    damageCommitted += 2;
                } else if (Card.listContainsCard("Kill Command", friendlyHand) && currentMana >= 3) {
                    toReturn.add(Card.getCardFromListByName("Kill Command", friendlyHand));
                    currentMana -= 3;
                    if (beastInPlay(friendlyBoard)) {
                        damageCommitted += 5;
                    } else {
                        damageCommitted += 3;
                    }
                } else {
                    //If none of the cases are met at this point, put together as much damage as possible and return.
                    break;
                }
            } else if (targetDamage == 3) {
                if (Card.listContainsCard("Quick Shot", friendlyHand) && currentMana >= 2) {
                    toReturn.add(Card.getCardFromListByName("Quick Shot", friendlyHand));
                    currentMana -= 2;
                    damageCommitted += 2;
                } else if (Card.listContainsCard("Kill Command", friendlyHand) && currentMana >= 3) {
                    toReturn.add(Card.getCardFromListByName("Kill Command", friendlyHand));
                    currentMana -= 3;
                    if (beastInPlay(friendlyBoard)) {
                        damageCommitted += 5;
                    } else {
                        damageCommitted += 3;
                    }
                } else {
                    //If none of the cases are met at this point, put together as much damage as possible and return.
                    break;
                }
            } else if (targetDamage <= 5 && targetDamage > 3) {
                if (Card.listContainsCard("Kill Command", friendlyHand)
                        && currentMana >= 3 && beastInPlay(friendlyBoard)) {
                    toReturn.add(Card.getCardFromListByName("Kill Command", friendlyHand));
                    currentMana -= 3;
                    damageCommitted += 5;
                } else if (Collections.frequency(friendlyHand, Card.getCardFromListByName("Quick Shot", friendlyHand)) == 2
                        && currentMana >= 4) {
                    toReturn.add(Card.getCardFromListByName("Quick Shot", friendlyHand));
                    currentMana -= 4;
                    damageCommitted += 6;
                } else if (Card.listContainsCard("Tundra Rhino", friendlyHand)
                        && Card.listContainsCard("Quick Shot", friendlyHand)
                        && currentMana >= 7) {
                    toReturn.add(Card.getCardFromListByName("Quick Shot", friendlyHand));
                    toReturn.add(Card.getCardFromListByName("Tundra Rhino", friendlyHand));
                    currentMana -= 7;
                    damageCommitted += 5;
                } else if (Card.listContainsCard("Tundra Rhino", friendlyHand)
                        && Card.listContainsCard("Kill Command", friendlyHand)
                        && currentMana >= 7) {
                    toReturn.add(Card.getCardFromListByName("Kill Command", friendlyHand));
                    toReturn.add(Card.getCardFromListByName("Tundra Rhino", friendlyHand));
                    //We don't need to check if there's a beast in hand- if there was the first case would have triggered.
                    currentMana -= 7;
                    damageCommitted += 5;
                } else if (Collections.frequency(friendlyHand, Card.getCardFromListByName("Quick Shot", friendlyHand)) == 2
                        && !beastInPlay(friendlyBoard) && currentMana >= 6) {
                    toReturn.add(Card.getCardFromListByName("Kill Command", friendlyHand));
                    toReturn.add(Card.getCardFromListByName("Kill Command", friendlyHand));
                    currentMana -= 6;
                    damageCommitted += 6;
                } else {
                    //If none of the cases are met at this point, put together as much damage as possible and return.
                    break;
                }
            } else if (targetDamage > 5) {
                //If there's no predetermined set of best combos to kill this minion (as its health is over 5)
                //we will instead iterate through our set of most efficient damage options, and select the best.
                if (Card.listContainsCard("Kill Command", friendlyHand)
                        && currentMana >= 3
                        && beastInPlay(friendlyBoard)) {
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
                            && !beastInPlay(friendlyBoard)) {
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
                    //If none of the cases are met at this point, put together as much damage as possible and return.
                    break;
                }
            }
        }

        return toReturn;
    }

    private boolean canValueTrade(Card friendlyMinion, List<Card> enemyBoard) {
        for (Card c : enemyBoard) {
            if ((c.getHealth() <= friendlyMinion.getAttack()
                && (friendlyMinion.getHealth() > c.getAttack()
                    || friendlyMinion.getAttack() < c.getAttack()))) {
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
                    && (friendlyMinion.getHealth() > c.getAttack()
                    || friendlyMinion.getAttack() < c.getAttack()))) {
                if (highestCost == null) {
                    highestCost = c;
                } else if (c.getCost() > highestCost.getCost()) {
                    highestCost = c;
                }
            }
        }
        return highestCost;
    }

    private List<Card> findBestTrades(List<Card> friendlyBoard, int tauntAttack, int tauntHp) {
        int damageCommitted = 0;
        List<Card> toReturn = new ArrayList<>();
        //Try to seek value trades first. If they aren't enough, start regular trades.
        for (Card c : friendlyBoard) {
            if (c.getHealth() > tauntAttack) {
                damageCommitted += c.getAttack();
                toReturn.add(c);
                if (damageCommitted >= tauntHp) {
                    break;
                }
            }
        }


        if (damageCommitted < tauntHp) {
            for (Card c : friendlyBoard) {
                if (!toReturn.contains(c)) {
                    damageCommitted += c.getAttack();
                    toReturn.add(c);
                    if (damageCommitted >= tauntHp) {
                        break;
                    }
                }
            }
        }

        return toReturn;
    }

    private boolean handContainsMinions(List<Card> friendlyHand) {
        for (Card c : friendlyHand) {
            if (c.getType().equalsIgnoreCase("minion")) {
                return true;
            }
        }
        return false;
    }

    private boolean buzzardTurnCriteriaMet(List<Card> friendlyHand, List<Card> friendlyBoard, int manaAvailable) {
        if (friendlyBoard.size() > 4
                || manaAvailable < 4
                || !Card.listContainsCard("Starving Buzzard", friendlyHand)
                || friendlyHand.size() > 6) {
            return false;
        } else {
            //We want to ensure that we can play at least one other beast along with the buzzard.
            //We want to avoid playing buzzard into buzzard, as it would be a waste of card draw.
            for (Card c : friendlyHand) {
                if (isBeast(c)
                        && c.getCost() <= (manaAvailable - 2)
                        && !c.getName().equalsIgnoreCase("Starving Buzzard")) {
                    return true;
                }
            }
            return false;
        }
    }

    private Card getBestBuzzardPlay(List<Card> friendlyHand, List<Card> friendlyBoard) {
        Card toReturn = null;
        int lowestCost = Integer.MAX_VALUE;
        for (Card c : friendlyHand) {
            if (c.getCost() < lowestCost
                && c.getType().equalsIgnoreCase("minion")) {
                lowestCost = c.getCost();
            }
        }
        for (Card c : friendlyHand) {
            if (toReturn == null) {
                if (c.getCost() == lowestCost
                    && c.getType().equalsIgnoreCase("minion")) {
                    toReturn = c;
                }
            } else if (c.getCost() == lowestCost
                    && cardRankings.get(c.getCardId()) < cardRankings.get(toReturn.getCardId())
                    && c.getType().equalsIgnoreCase("minion")) {
                toReturn = c;
            }
        }
        return toReturn;
    }

    //This method informs the analyzer that it should pull the current game state and generate messages
    public void notifyStart(int currentManaCount, boolean alreadyAttacked) {
        //TODO: Add special cases for when the player has The Coin
        List<Card> friendlyHand = new ArrayList<>(state.getFriendlyHand());
        List<Card> friendlyBoard = new ArrayList<>(state.getFriendlyBoard());
        List<Card> enemyBoard = new ArrayList<>(state.getEnemyBoard());

        // The number of minions on our board. This may fluctuate throughout the turn, so we'll keep track of it here.
        int boardSize = friendlyBoard.size();

        // If we have the coin, we want to play it ASAP.
        if (Card.listContainsCard("The Coin", friendlyHand)) {
            currentManaCount++;
            messageQueue.add("Play The Coin.");
        }

        // The amount of damage we can produce from our hand.
        int damageFromHand = damageFromHandCalculation(friendlyHand, currentManaCount, friendlyBoard);

        // The amount of damage we can produce from our board.
        int damageFromBoard = damageFromBoardCalculation(friendlyBoard);

        // Indicates if we have a Tundra Rhino in play, which allows our beasts to attack the turn we play them.
        boolean rhinoInPlay = Card.listContainsCard("Tundra Rhino", friendlyBoard);

        // Indicates if we have a timber wolf in play, which buffs the attack of our other beasts by one.
        boolean timberWolfInPlay = Card.listContainsCard("Timber Wolf", friendlyBoard);

        // Indicates if we have a starving buzzard in play, which allows us to draw a card whenever we summon a beast.
        boolean buzzardInPlay = Card.listContainsCard("Starving Buzzard", friendlyBoard);

        // Indicates if we have an eaglehorn bow equipped.
        boolean bowEquipped = state.isBowEquipped();

        boolean heroPowerUsed = false;

        if (enemyBoard.size() == 0) {
            //If the opponent has no cards in play, we want to play out the best, highest cost cards from our hand.
            //We also want to send all of our attackers at the opponent's face.
            List<Card> attackers = new ArrayList<>(friendlyBoard);
            while (currentManaCount > 0) {
                if (!handContainsMinions(friendlyHand)) {
                    if (currentManaCount >= 2 && damageFromHand > 0) {
                        List<Card> directDamageCards = getHighestDamageCombo(friendlyHand, currentManaCount, friendlyBoard);
                        for (Card c : directDamageCards) {
                            if (c.getName().equalsIgnoreCase("eaglehorn bow")) {
                                messageQueue.add("Equip " + c.getName() + ".");
                                messageQueue.add("Attack the opponent's face with your " + c.getName() + ".");
                            } else {
                                messageQueue.add("Use " + c.getName() + " on the opponent's face.");
                            }
                            friendlyHand.remove(c);
                            currentManaCount -= c.getCost();
                        }
                    }
                }

                if (buzzardTurnCriteriaMet(friendlyHand, friendlyBoard, currentManaCount)) {
                    if (!alreadyAttacked) {
                        for (Card c : friendlyBoard) {
                            messageQueue.add("Attack the opponent's face with " + c.getName() + ".");
                        }

                        if (bowEquipped) {
                            messageQueue.add("Attack the opponent's face with your Eaglehorn Bow.");
                        }
                    }
                    messageQueue.add("Play Starving Buzzard.");
                    friendlyHand.remove(Card.getCardFromListByName("Starving Buzzard", friendlyHand));
                    currentManaCount -= 2;
                    boardSize++;

                    Card bestBeast = getBestBuzzardPlay(friendlyHand, friendlyBoard);
                    messageQueue.add("Play " + bestBeast.getName() + ".");
                    currentManaCount -= bestBeast.getCost();
                    if (bestBeast.getName().equalsIgnoreCase("Wolpertinger")
                        || bestBeast.getName().equalsIgnoreCase("Alleycat")) {
                        state.notifyAfterNDraws(2, currentManaCount);
                        return;
                    } else {
                        state.notifyAfterNDraws(1, currentManaCount);
                        return;
                    }
                } else if (buzzardInPlay) {
                    while (beastInHand(friendlyHand)) {
                        Card bestBeast = getBestBuzzardPlay(friendlyHand, friendlyBoard);
                        if (bestBeast == null) {
                            break;
                        } else if (bestBeast.getCost() > currentManaCount) {
                            break;
                        } else {
                            messageQueue.add("Play " + bestBeast.getName() + ".");
                            currentManaCount -= bestBeast.getCost();
                            if (rhinoInPlay) {
                                messageQueue.add("Attack the opponent's face with " + bestBeast.getName() + ".");
                            }
                            if ((bestBeast.getName().equalsIgnoreCase("Wolpertinger")
                                    || bestBeast.getName().equalsIgnoreCase("Alleycat"))
                                    && boardSize <= 5) {
                                state.notifyAfterNDraws(2, currentManaCount);
                                return;
                            } else {
                                state.notifyAfterNDraws(1, currentManaCount);
                                return;
                            }
                        }
                    }
                }

                Card toPlay = null;
                int highestCost = 0;
                for (Card c : friendlyHand) {
                    if (c.getCost() > highestCost
                            && c.getCost() <= currentManaCount) {
                        if (!(c.getName().equalsIgnoreCase("Master's Call")
                                && friendlyHand.size() > 8)) {
                            highestCost = c.getCost();
                        }
                    }
                }

                //If we have nothing to play, just exit the loop.
                if (highestCost == 0) {
                    break;
                } else if ((highestCost == 1 && currentManaCount == 2)
                            && friendlyBoard.size() > 0
                            && !Card.listContainsCard("The Coin", friendlyHand)) {
                    messageQueue.add("Use your hero power.");
                    heroPowerUsed = true;
                    currentManaCount -= 2;
                } else {
                    if (friendlyHand.isEmpty()) {
                        messageQueue.add("Use your hero power.");
                        heroPowerUsed = true;
                        currentManaCount -= 2;
                        break;
                    }
                    for (Card c : friendlyHand) {
                        if (toPlay == null) {
                            if (c.getCost() == highestCost) {
                                if (!(c.getName().equalsIgnoreCase("Master's Call")
                                        && friendlyHand.size() > 8)) {
                                    toPlay = c;
                                }
                            }
                        } else if (c.getCost() == highestCost
                                && cardRankings.get(c.getCardId()) < cardRankings.get(toPlay.getCardId())) {
                            if (!(c.getName().equalsIgnoreCase("Master's Call")
                                    && friendlyHand.size() > 8)) {
                                toPlay = c;
                            }
                        }
                    }
                    if (toPlay != null) {
                        if (toPlay.getType().equalsIgnoreCase("minion") && boardSize < 7) {
                                messageQueue.add("Play " + toPlay.getName() + ".");
                                friendlyHand.remove(toPlay);
                                currentManaCount -= toPlay.getCost();
                                boardSize++;

                                if (buzzardInPlay) {
                                    state.notifyAfterNDraws(1, currentManaCount);
                                    return;
                                }

                                if (toPlay.getName().equalsIgnoreCase("Tundra Rhino")) {
                                    rhinoInPlay = true;
                                } else if (toPlay.getName().equalsIgnoreCase("Timber Wolf")) {
                                    timberWolfInPlay = true;
                                } else if (toPlay.getName().equalsIgnoreCase("Wolpertinger")
                                            || toPlay.getName().equalsIgnoreCase("Alleycat")) {
                                    boardSize++;
                                } else if (toPlay.getName().equalsIgnoreCase("Starving Buzzard")) {
                                    buzzardInPlay = true;
                                }

                                if (rhinoInPlay) {
                                    if (alreadyAttacked) {
                                        messageQueue.add("Attack the opponent's face with " + toPlay.getName() + ".");
                                    } else {
                                        attackers.add(toPlay);
                                    }
                                }
                        } else if (!toPlay.getType().equalsIgnoreCase("minion")) {
                            //If we're playing a non-minion card from our hand, we need to check what it is to
                            //deal with potential special cases.
                            switch (toPlay.getName()) {
                                case "Eaglehorn Bow":
                                    messageQueue.add("Equip Eaglehorn Bow.");
                                    messageQueue.add("Attack the opponent's face with your Eaglehorn Bow.");
                                    break;
                                case "Master's Call":
                                    messageQueue.add("Play Master's Call.");
                                    currentManaCount -= toPlay.getCost();
                                    state.getFriendlyHand().remove(toPlay);
                                    state.notifyAfterNDraws(3, currentManaCount);
                                    if (!alreadyAttacked) {
                                        for (Card c : friendlyBoard) {
                                            messageQueue.add("Attack the opponent's face with " + c.getName() + ".");
                                        }

                                        if (bowEquipped) {
                                            messageQueue.add("Attack the opponent's face with your Eaglehorn Bow.");
                                        }
                                    }
                                    return;
                                case "Freezing Trap":
                                    messageQueue.add("Play Freezing Trap.");
                                    break;
                                default:
                                    messageQueue.add("Use " + toPlay.getName() + " on the opponent's face.");
                                    break;
                            }
                            friendlyHand.remove(toPlay);
                            currentManaCount -= toPlay.getCost();
                        } else if (boardSize >= 7) {
                            if (currentManaCount >= 2) {
                                messageQueue.add("Use your hero power.");
                                heroPowerUsed = true;
                                currentManaCount -= 2;
                            }
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            if (!alreadyAttacked) {
                for (Card c : attackers) {
                    messageQueue.add("Attack the opponent's face with " + c.getName() + ".");
                }

                if (bowEquipped) {
                    messageQueue.add("Attack the opponent's face with your Eaglehorn Bow.");
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
            int totalTauntHealth = 0;

            if (enemyTauntMinions.size() > 0) {
                //When removing the opponent's taunt minions, we would prefer to utilize direct damage from our
                //hand over our minions when possible, as maintaining control of the board is always our highest
                //priority.
                for (Card c : enemyBoard) {
                    if (c.getMechanics().contains("Taunt")) {
                        totalTauntHealth += c.getHealth();
                    }
                }
                if (damageFromHand >= totalTauntHealth) {
                    while (damageFromHand >= totalTauntHealth
                            && totalTauntHealth > 0
                            && damageFromHand > 0) {
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

                        List<Card> cardsToKill = killFromHand(friendlyHand, highestHp, currentManaCount, friendlyBoard);
                        for (Card d : cardsToKill) {
                            messageQueue.add("Use " + d.getName() + " on the opponent's " + highestHp.getName() + ".");
                            friendlyHand.remove(d);
                            currentManaCount -= d.getCost();
                            if (d.getName().equalsIgnoreCase("Tundra Rhino")) {
                                rhinoInPlay = true;
                                boardSize++;
                            }
                        }
                        //Update the amount of damage we have available from our hand.
                        damageFromHand = damageFromHandCalculation(friendlyHand, currentManaCount, friendlyBoard);
                        totalTauntHealth -= highestHp.getHealth();
                        enemyBoard.remove(highestHp);
                    }
                } else if (damageFromHand + damageFromBoard >= totalTauntHealth) {
                    //Use all of our hand damage and the most efficient possible trades to kill the taunt
                    Card highestHp = null;
                    for (Card c : enemyTauntMinions) {
                        if (highestHp == null) {
                            highestHp = c;
                        } else if (c.getHealth() > highestHp.getHealth()) {
                            highestHp = c;
                        }
                    }

                    List<Card> cardsToKill = killFromHand(friendlyHand, highestHp, currentManaCount, friendlyBoard);
                    int tauntHp = highestHp.getHealth();
                    for (Card d : cardsToKill) {
                        messageQueue.add("Use " + d.getName() + " on the opponent's " + highestHp.getName() + ".");
                        friendlyHand.remove(d);
                        currentManaCount -= d.getCost();
                        switch (d.getName()) {
                            case "Kill Command":
                                if (beastInPlay(friendlyBoard)) {
                                    tauntHp -= 5;
                                } else {
                                    tauntHp -= 3;
                                }
                                break;
                            case "Quick Shot":
                                tauntHp -= 3;
                            case "Tundra Rhino":
                                rhinoInPlay = true;
                                boardSize++;
                                tauntHp -= 2;
                            default:
                                tauntHp -=3;
                                break;
                        }
                    }

                    if (tauntHp > 0) {
                        //We want to find the most efficient set of minions on our board to throw into the taunt.
                        List<Card> toTrade = findBestTrades(friendlyBoard, highestHp.getAttack(), tauntHp);
                        for (Card c : toTrade) {
                            messageQueue.add("Attack the opponent's "
                                    + highestHp.getName()
                                    + " with your "
                                    + c.getName() + ".");
                            friendlyBoard.remove(c);
                        }
                    }
                    enemyBoard.remove(highestHp);
                }
            }
            List<Card> faceAttackers = new ArrayList<>();
            if (bowEquipped && !alreadyAttacked) {
                //If we have a bow equipped, we want to prioritize trading into the highest attack minion
                //the opponent has in play.
                Card highestAttack = null;
                for (Card e : enemyBoard) {
                    if (highestAttack == null
                            && e.getHealth() <= 3) {
                        highestAttack = e;
                    } else if (e.getHealth() <= 3
                            && e.getAttack() >= highestAttack.getAttack()) {
                        highestAttack = e;
                    }
                }

                if (highestAttack == null) {
                    messageQueue.add("Attack the opponent's face with your Eaglehorn Bow.");
                } else {
                    messageQueue.add("Attack the opponent's "
                            + highestAttack.getName()
                            + " with your Eaglehorn Bow.");
                    enemyBoard.remove(highestAttack);
                }
            }
            for (Card c : friendlyBoard) {
                if (!alreadyAttacked) {
                    if (canValueTrade(c, enemyBoard)) {
                        messageQueue.add("Attack the opponent's "
                                + getValueTradeTarget(c, enemyBoard).getName()
                                + " with your " + c.getName() + ".");
                        enemyBoard.remove(getValueTradeTarget(c, enemyBoard));
                    } else {
                        faceAttackers.add(c);
                    }
                }
            }
            //After this, we'll proceed as normal, playing out the best, highest-cost minions from our hand.
            while (currentManaCount > 0) {
                if (!handContainsMinions(friendlyHand)) {
                    if (currentManaCount >= 2 && damageFromHand > 0) {
                        List<Card> directDamageCards = getHighestDamageCombo(friendlyHand, currentManaCount, friendlyBoard);
                        state.damageOpponent(damageFromHandCalculation(friendlyHand, currentManaCount, friendlyBoard));
                        for (Card c : directDamageCards) {
                            if (c.getName().equalsIgnoreCase("eaglehorn bow")) {
                                messageQueue.add("Equip " + c.getName() + ".");
                                messageQueue.add("Attack the opponent's face with your " + c.getName() + ".");
                            } else {
                                messageQueue.add("Use " + c.getName() + " on the opponent's face.");
                            }
                            friendlyHand.remove(c);
                            currentManaCount -= c.getCost();
                        }
                    }
                }

                if (buzzardTurnCriteriaMet(friendlyHand, friendlyBoard, currentManaCount)) {
                    if (!alreadyAttacked) {
                        for (Card c : faceAttackers) {
                            messageQueue.add("Attack the opponent's face with " + c.getName() + ".");
                        }
                    }
                    messageQueue.add("Play Starving Buzzard.");
                    if (rhinoInPlay) {
                        messageQueue.add("Attack the opponent's face with Starving Buzzard.");
                    }
                    friendlyHand.remove(Card.getCardFromListByName("Starving Buzzard", friendlyHand));
                    currentManaCount -= 2;
                    boardSize++;

                    Card bestBeast = getBestBuzzardPlay(friendlyHand, friendlyBoard);
                    messageQueue.add("Play " + bestBeast.getName() + ".");
                    if (rhinoInPlay) {
                        if (canValueTrade(bestBeast, enemyBoard)) {
                            messageQueue.add("Attack the opponent's "
                                    + getValueTradeTarget(bestBeast, enemyBoard).getName()
                                    + " with your " + bestBeast.getName() + ".");
                            enemyBoard.remove(getValueTradeTarget(bestBeast, enemyBoard));
                        } else {
                            messageQueue.add("Attack the opponent's face with " + bestBeast.getName() + ".");
                        }
                    }
                    currentManaCount -= bestBeast.getCost();
                    if (bestBeast.getName().equalsIgnoreCase("Wolpertinger")
                            || bestBeast.getName().equalsIgnoreCase("Alleycat")) {
                        state.notifyAfterNDraws(2, currentManaCount);
                        return;
                    } else {
                        state.notifyAfterNDraws(1, currentManaCount);
                        return;
                    }
                } else if (buzzardInPlay && friendlyBoard.size() < 7) {
                    while (beastInHand(friendlyHand)) {
                        Card bestBeast = getBestBuzzardPlay(friendlyHand, friendlyBoard);
                        if (bestBeast == null) {
                            break;
                        } else if (bestBeast.getCost() > currentManaCount) {
                            break;
                        } else {
                            messageQueue.add("Play " + bestBeast.getName() + ".");
                            if (rhinoInPlay) {
                                if (canValueTrade(bestBeast, enemyBoard)) {
                                    messageQueue.add("Attack the opponent's "
                                            + getValueTradeTarget(bestBeast, enemyBoard).getName()
                                            + " with your " + bestBeast.getName() + ".");
                                    enemyBoard.remove(getValueTradeTarget(bestBeast, enemyBoard));
                                } else {
                                    messageQueue.add("Attack the opponent's face with " + bestBeast.getName() + ".");
                                }
                            }
                            currentManaCount -= bestBeast.getCost();
                            if ((bestBeast.getName().equalsIgnoreCase("Wolpertinger")
                                    || bestBeast.getName().equalsIgnoreCase("Alleycat"))
                                    && friendlyBoard.size() <= 5) {
                                state.notifyAfterNDraws(2, currentManaCount);
                                return;
                            } else {
                                state.notifyAfterNDraws(1, currentManaCount);
                                return;
                            }
                        }
                    }
                }

                Card toPlay = null;
                int highestCost = 0;
                for (Card c : friendlyHand) {
                    if (c.getCost() > highestCost
                            && c.getCost() <= currentManaCount) {
                        if (!(c.getName().equalsIgnoreCase("Master's Call")
                                && friendlyHand.size() > 8)) {
                            highestCost = c.getCost();
                        }
                    }
                }

                //If we have nothing to play, just exit the loop.
                if (highestCost == 0) {
                    break;
                } else if (highestCost == 1 && currentManaCount == 2) {
                    messageQueue.add("Use your hero power.");
                    heroPowerUsed = true;
                    currentManaCount -= 2;
                } else {
                    if (friendlyHand.isEmpty()) {
                        messageQueue.add("Use your hero power.");
                        heroPowerUsed = true;
                        currentManaCount -= 2;
                        break;
                    }
                    for (Card c : friendlyHand) {
                        if (toPlay == null) {
                            if (c.getCost() == highestCost) {
                                if (!(c.getName().equalsIgnoreCase("Master's Call")
                                        && friendlyHand.size() > 8)) {
                                    toPlay = c;
                                }
                            }
                        } else if (c.getCost() == highestCost
                                && cardRankings.get(c.getCardId()) < cardRankings.get(toPlay.getCardId())) {
                            if (!(c.getName().equalsIgnoreCase("Master's Call")
                                    && friendlyHand.size() > 8)) {
                                toPlay = c;
                            }
                        }
                    }
                    if (toPlay != null) {
                        if (toPlay.getType().equalsIgnoreCase("minion") && boardSize < 7) {
                            if (boardSize < 7) {
                                messageQueue.add("Play " + toPlay.getName() + ".");
                                friendlyHand.remove(toPlay);
                                currentManaCount -= toPlay.getCost();
                                boardSize++;

                                if (buzzardInPlay) {
                                    state.notifyAfterNDraws(1, currentManaCount);
                                    return;
                                }

                                if (toPlay.getName().equalsIgnoreCase("Tundra Rhino")) {
                                    rhinoInPlay = true;
                                } else if (toPlay.getName().equalsIgnoreCase("Timber Wolf")) {
                                    timberWolfInPlay = true;
                                } else if (toPlay.getName().equalsIgnoreCase("Wolpertinger")
                                        || toPlay.getName().equalsIgnoreCase("Alleycat")) {
                                    boardSize++;
                                } else if (toPlay.getName().equalsIgnoreCase("Starving Buzzard")) {
                                    buzzardInPlay = true;
                                }

                                if (rhinoInPlay) {
                                    if (alreadyAttacked) {
                                        if (canValueTrade(toPlay, enemyBoard)) {
                                            messageQueue.add("Attack the opponent's "
                                                    + getValueTradeTarget(toPlay, enemyBoard).getName()
                                                    + " with your " + toPlay.getName() + ".");
                                            enemyBoard.remove(getValueTradeTarget(toPlay, enemyBoard));
                                        } else {
                                            messageQueue.add("Attack the opponent's face with " + toPlay.getName() + ".");
                                        }
                                    } else {
                                        if (canValueTrade(toPlay, enemyBoard)) {
                                            messageQueue.add("Attack the opponent's "
                                                    + getValueTradeTarget(toPlay, enemyBoard).getName()
                                                    + " with your " + toPlay.getName() + ".");
                                            enemyBoard.remove(getValueTradeTarget(toPlay, enemyBoard));
                                        } else {
                                            faceAttackers.add(toPlay);
                                        }
                                    }

                                }
                            }
                        } else if (!toPlay.getType().equalsIgnoreCase("minion")) {
                            //If we're playing a non-minion card from our hand, we need to check what it is to
                            //deal with potential special cases.
                            switch (toPlay.getName()) {
                                case "Eaglehorn Bow":
                                    messageQueue.add("Equip Eaglehorn Bow.");
                                    messageQueue.add("Attack the opponent's face with your Eaglehorn Bow.");
                                    break;
                                case "Master's Call":
                                    if (!alreadyAttacked) {
                                        for (Card c : faceAttackers) {
                                            messageQueue.add("Attack the opponent's face with " + c.getName() + ".");
                                        }
                                    }
                                    messageQueue.add("Play Master's Call.");
                                    currentManaCount -= toPlay.getCost();
                                    state.getFriendlyHand().remove(toPlay);
                                    state.notifyAfterNDraws(3, currentManaCount);
                                    return;
                                case "Freezing Trap":
                                    messageQueue.add("Play Freezing Trap.");
                                    break;
                                default:
                                    messageQueue.add("Use " + toPlay.getName() + " on the opponent's face.");
                                    break;
                            }
                            friendlyHand.remove(toPlay);
                            currentManaCount -= toPlay.getCost();
                        } else if (boardSize >= 7) {
                            if (currentManaCount >= 2) {
                                messageQueue.add("Use your hero power.");
                                heroPowerUsed = true;
                                currentManaCount -= 2;
                            }
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            if (!alreadyAttacked && totalTauntHealth == 0) {
                for (Card c : faceAttackers) {
                    messageQueue.add("Attack the opponent's face with " + c.getName() + ".");
                }
            }
        }

        if (!heroPowerUsed && currentManaCount >= 2) {
            messageQueue.add("Use your hero power.");
        }

        // Obviously, this is the last thing we will do each turn.
        messageQueue.add("Pass the turn.");
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