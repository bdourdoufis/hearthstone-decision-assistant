package com.hsda.service;

import com.hsda.models.Card;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CardFetcherService {
    final String CARD_GET_BASE_URL = "https://omgvamp-hearthstone-v1.p.rapidapi.com/cards/";

    public CardFetcherService() {

    }

    public Card getCardInfo(String cardId) throws IOException, InterruptedException {
        String formattedName = cardId.replaceAll(" ", "%20");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CARD_GET_BASE_URL + formattedName))
                .header("x-rapidapi-key", "d3fdde6f41msh444a83fc6f2568ap1074ecjsnd405d2063274")
                .header("x-rapidapi-host", "omgvamp-hearthstone-v1.p.rapidapi.com")
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject jsonObject = new JSONObject(response.body().substring(1, response.body().length() - 1));
        String cardName = jsonObject.getString("name");
        String cardType = jsonObject.getString("type");
        if (cardType.equalsIgnoreCase("hero")) {
            return new Card(cardName, cardId, cardType, 0, 0, 0, new ArrayList<>());
        }
        int cardCost = jsonObject.getInt("cost");
        int cardAttack = 0;
        int cardHealth = 0;
        List<String> mechanics = new ArrayList<String>();

        if (cardType.equalsIgnoreCase("minion")) {
            cardAttack = jsonObject.getInt("attack");
            cardHealth = jsonObject.getInt("health");
            try {
                JSONArray mechanicsArray = jsonObject.getJSONArray("mechanics");
                for (int i = 0; i < mechanicsArray.length(); i++) {
                    JSONObject mechanicObject = mechanicsArray.getJSONObject(i);
                    mechanics.add(mechanicObject.getString("name"));
                }
            } catch (JSONException e) {
                //If we reached this point, the given card's API response does not contain any mechanics
                //as such, we'll just use the empty list.
            }
        }

        return new Card(cardName, cardId, cardType, cardCost, cardAttack, cardHealth, mechanics);
    }

    public List<Card> getDeckCards() throws IOException, InterruptedException {
        List<Card> deck = new ArrayList<>();
        List<String> cardIds = Arrays.asList("CFM_315","LOOT_258","OG_179","BAR_031","DS1_175",
                "SCH_133","DRG_071","EX1_611","BAR_745","CORE_BRM_013","CORE_EX1_531",
                "CS2_237","EX1_536","EX1_539","TRL_339","DS1_178");
        for (String id : cardIds) {
            deck.add(getCardInfo(id));
        }
        return deck;
    }
}
