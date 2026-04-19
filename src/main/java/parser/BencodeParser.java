package parser;

import type.BencodeString;
import type.DictState;
import type.ListState;
import type.StructureState;

import java.util.*;
import java.util.logging.Logger;

public class BencodeParser {

    private final ByteCursor byteCursor;
    private static final Logger logger = Logger.getLogger(BencodeParser.class.getName());

    private static final char STRING_DELIMITER = ':';
    private static final char INTEGER_START = 'i';
    private static final char LIST_START = 'l';
    private static final char DICT_START = 'd';
    private static final char END = 'e';

    public BencodeParser(String bencodedString) {
        byteCursor = new ByteCursor(bencodedString.getBytes());
    }

    public BencodeParser(byte[] bencode) {
        byteCursor = new ByteCursor(bencode);
    }

    public Object parseBencode() {
        Deque<StructureState> structureStack = new ArrayDeque<>();
        Object rootValue = null;
        while (byteCursor.hasRemaining()) {
            Object value = null;
            StructureState newStructure = null;
            char curr = (char) byteCursor.peekByte();
            switch (curr) {
                case INTEGER_START -> value = parseBencodedInteger();
                case LIST_START -> {
                    byteCursor.readByte();
                    newStructure = new ListState();
                }
                case DICT_START -> {
                    byteCursor.readByte();
                    newStructure = new DictState();
                }
                case END -> {
                    byteCursor.readByte();
                    if (structureStack.isEmpty()) {
                        throw new RuntimeException("Invalid Bencode: Unexpected 'e'");
                    }
                    value = structureStack.pop().getStructure();
                }
                default -> value = parseBencodedString();
            }
            if (newStructure != null) {
                structureStack.push(newStructure);
            } else if (value != null) {
                if (structureStack.isEmpty()) {
                    rootValue = value;
                    break;
                } else {
                    structureStack.peek().addElement(value);
                }
            }
        }
        if (!structureStack.isEmpty()) {
            throw new RuntimeException("Invalid Bencode: Unclosed structures remaining");
        }
        if (byteCursor.hasRemaining()) {
            throw new RuntimeException("Invalid Bencode: Trailing garbage detected after root element.");
        }
        return rootValue;
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
