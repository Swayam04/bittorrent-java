package parser;

import type.BencodeString;

import java.util.*;
import java.util.logging.Logger;

public class BencodeParser {

    private Deque<Object> structureStack;
    private final ByteCursor byteCursor;
    private Deque<BencodeString> keyStack = new ArrayDeque<>();

    private static final Logger logger = Logger.getLogger(BencodeParser.class.getName());

    private static final char STRING_DELIMITER = ':';
    private static final char INTEGER_START = 'i';
    private static final char LIST_START = 'l';
    private static final char DICT_START = 'd';
    private static final char END = 'e';

    public BencodeParser(String bencodedString) {
        structureStack = new ArrayDeque<>();
        byteCursor = new ByteCursor(bencodedString.getBytes());
    }

    public BencodeParser(byte[] bencode) {
        structureStack = new ArrayDeque<>();
        byteCursor = new ByteCursor(bencode);
    }

    public String parseBencode() {
        while (byteCursor.hasRemaining()) {
            Object value = null;
            char curr = (char) byteCursor.peekByte();
            switch (curr) {
                case INTEGER_START -> value = parseBencodedInteger();
                case LIST_START -> {
                    structureStack.push(new ArrayList<>());
                    byteCursor.readByte();
                }
                case DICT_START -> {
                    structureStack.push(new TreeMap<BencodeString, Object>());
                    byteCursor.readByte();
                }
                case END -> {
                    if (structureStack.isEmpty()) {
                        throw new RuntimeException("Invalid Bencode");
                    }
                    value = structureStack.pop();
                    byteCursor.readByte();
                }
                default -> value = parseBencodedString();
            }
            if (value != null) {
                if (structureStack.isEmpty()) {
                    return value.toString();
                }
                Object structure = structureStack.peek();
                if (structure instanceof List) {
                    ((List) structure).add(value);
                } else if (structure instanceof Map) {
                    if (keyStack.isEmpty()) {
                        if (!(value instanceof BencodeString)) {
                            throw new RuntimeException("Invalid Bencode! Key for Dict not of type String");
                        }
                        keyStack.push((BencodeString) value);
                    } else {
                        if (keyStack.isEmpty()) {
                            throw new RuntimeException("Invalid Bencode");
                        }
                        ((Map) structure).put(keyStack.pop(), value);
                    }
                }
            }
        }
        return "";
    }

    public Long parseBencodedInteger() {
        int start = byteCursor.position();
        byteCursor.readByte();
        StringBuilder numString = new StringBuilder();
        boolean ended = false;
        while(byteCursor.hasRemaining() && !ended) {
            char curr = byteCursor.readChar();
            if (curr == END) {
                ended = true;
            } else {
                numString.append(curr);
            }
        }
        if (!ended) {
            throw new RuntimeException("Invalid Bencode Integer at position: " + start);
        }
        try {
            String value = numString.toString();
            if (value.startsWith("0") && value.length() > 1 || value.startsWith("-0")) {
                throw new RuntimeException("Invalid Bencode Integer at position: " + start);
            }
            return Long.parseLong(value);
        } catch (Exception e) {
            throw new RuntimeException("Invalid Bencode Integer at position: " + start, e);
        }
    }

    public BencodeString parseBencodedString() {
        int start = byteCursor.position();
        boolean ended = false;
        StringBuilder lenString = new StringBuilder();
        while (byteCursor.hasRemaining() && !ended) {
            char curr = byteCursor.readChar();
            if (curr == STRING_DELIMITER) {
                ended = true;
            } else {
                lenString.append(curr);
            }
        }
        if (!ended) {
            throw new RuntimeException("Invalid Bencode String at position: " + start);
        }
        int len = Integer.parseInt(lenString.toString());
        if (byteCursor.remaining() < len) {
            throw new RuntimeException("Invalid Bencode String at position: " + start);
        }
        return new BencodeString(byteCursor.readBytes(len));
    }


}
