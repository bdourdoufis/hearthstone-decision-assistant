package com.hsda.models;

import java.util.List;

public class Card {
    private String name;
    private String cardId;
    private String type;
    private int cost;
    private int attack;
    private int health;
    private List<String> mechanics;

    public Card(Card other) {
        this.name = other.getName();
        this.cardId = other.getCardId();
        this.type = other.getType();
        this.cost = other.getCost();
        this.attack = other.getAttack();
        this.health = other.getHealth();
        this.mechanics = other.getMechanics();
    }

    public Card(String name, String cardId, String type, int cost, int attack, int health, List<String> mechanics) {
        this.name = name;
        this.cardId = cardId;
        this.type = type;
        this.cost = cost;
        this.attack = attack;
        this.health = health;
        this.mechanics = mechanics;
    }

    public static boolean listContainsCard(String cardName, List<Card> list) {
        for (Card c : list) {
            if (c.getName().equalsIgnoreCase(cardName)) {
                return true;
            }
        }
        return false;
    }

    public static Card getCardFromListByName(String cardName, List<Card> list) {
        for (Card c : list) {
            if (c.getName().equalsIgnoreCase(cardName)) {
                return c;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public int getAttack() {
        return attack;
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public List<String> getMechanics() {
        return mechanics;
    }

    public void setMechanics(List<String> mechanics) {
        this.mechanics = mechanics;
    }

    @Override
    public String toString() {
        return "Card: " + name + ", Type: " + type + ", Cost: " + cost
                + ", Attack: " + attack + ", Health: " + health + ", Mechanics: " + mechanics.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Card) {
            return cardId.equals(((Card) o).getCardId());
        }

        return false;
    }
}
