import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import parser.BencodeParser;
import type.BencodeString;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
// import com.dampcake.bencode.Bencode; - available if you need it!

public class Main {
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(BencodeString.class, (JsonSerializer<BencodeString>) (src, typeOfSrc, context) -> {
        return context.serialize(src.toString());}).create();

    public static void main(String[] args) throws Exception {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.err.println("Logs from your program will appear here!");

        String command = args[0];
        if ("decode".equals(command)) {
            String bencodedValue = args[1];
            Object decoded;
            try {
                decoded = new BencodeParser(bencodedValue).parseBencode();
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
                return;
            }
            System.out.println(gson.toJson(decoded));

        } else if ("info".equals(command)) {
            String pathString = args[1];
            byte[] fileContents = Files.readAllBytes(Paths.get(pathString));

            @SuppressWarnings("unchecked")
            Map<BencodeString, Object> decoded = (Map<BencodeString, Object>) new BencodeParser(fileContents).parseBencode();
            String trackerUrl = decoded.get(BencodeString.of("announce")).toString();

            @SuppressWarnings("unchecked")
            Map<BencodeString, Object> infoDict = (Map<BencodeString, Object>) decoded.get(BencodeString.of("info"));
            Long length = (Long) infoDict.get(BencodeString.of("length"));

            System.out.println("Tracker URL: " + trackerUrl);
            System.out.println("Length: " + length);
        } else {
            System.out.println("Unknown command: " + command);
        }

    }
}
