package com.hsda;

import com.hsda.analyzer.GameStateAnalyzer;
import com.hsda.models.GameState;
import com.hsda.service.CardFetcherService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Retrieving card data, please wait...");
        GameState state = new GameState();
        GameStateAnalyzer analyzer = new GameStateAnalyzer(state);
        Scanner example = new Scanner(System.in);
	    System.out.println("Please provide link to Zone.log file: ");
	    String path = example.nextLine();
	    File zoneLog = new File(path);

	    if (!zoneLog.exists()) {
	        System.out.println("Waiting for zone log to be generated...");
        }
	    while(!zoneLog.exists()) {
	        sleep(1);
        }

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));
        BufferedReader reader = new BufferedReader(new InputStreamReader(bis, StandardCharsets.UTF_8));

        while (true) {
            if (reader.ready()) {
                String nextLine = reader.readLine();
                if (nextLine.contains("BEGIN_MULLIGAN")) {
                    state.beginMulligan();
                } else if (state.isInMulligan() && nextLine.contains("tag=NUM_CARDS_DRAWN_THIS_TURN value=1")) {
                    state.beginGame();
                } else if (nextLine.contains("TRANSITIONING card")) {
                    if (nextLine.contains("to FRIENDLY HAND")) {
                        state.addCardToHand(nextLine);
                    } else if (nextLine.contains("to FRIENDLY DECK")
                            && !nextLine.contains("(Hero)")
                            && !nextLine.contains("(Hero Power)")) {
                        nextLine = reader.readLine();
                        if (nextLine != null) {
                            if (nextLine.contains("from FRIENDLY HAND -> FRIENDLY DECK")) {
                                state.mulliganCard(nextLine);
                            }
                        }
                    } else if (nextLine.contains("to FRIENDLY PLAY")
                            && !nextLine.contains("(Hero)")
                            && !nextLine.contains("(Hero Power)")) {
                        state.addCardToFriendlyBoard(nextLine);
                    } else if (nextLine.contains("to FRIENDLY GRAVEYARD")
                            && !nextLine.contains("(Hero)")
                            && !nextLine.contains("(Hero Power)")) {
                        state.cardToFriendlyGraveyard(nextLine);
                    } else if (nextLine.contains("to FRIENDLY SECRET")) {
                        state.addFriendlySecret(nextLine);
                    } else if (nextLine.contains("to OPPOSING HAND")) {
                        state.addCardToOpposingHand();
                    } else if (nextLine.contains("to OPPOSING PLAY")
                            && !nextLine.contains("(Hero)")
                            && !nextLine.contains("(Hero Power)")) {
                        state.addCardToOpposingBoard(nextLine);
                    } else if (nextLine.contains("to OPPOSING GRAVEYARD")
                            && !nextLine.contains("(Hero)")
                            && !nextLine.contains("(Hero Power)")) {
                        state.cardToOpposingGraveyard(nextLine);
                    } else {
                        //Dummy call, we should ignore everything that hits this case
                        //could make a verbose mode?
                        //System.out.println(nextLine);
                    }
                } else if (nextLine.contains("(Hero) -> OPPOSING GRAVEYARD")) {
                    System.out.println("Game over, victory!");
                    state.gameEnded();
                } else if (nextLine.contains("(Hero) -> FRIENDLY GRAVEYARD")) {
                    System.out.println("Game over, defeat.");
                    state.gameEnded();
                }
            } else {
                sleep(10);
            }

            // If the analyzer has any messages queued, print them.
            while(analyzer.hasMessage()) {
                System.out.println(analyzer.getMessage());
            }
        }

    }
}
