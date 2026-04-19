package parser;

import type.BencodeString;
import type.DictState;
import type.ListState;
import type.StructureState;

import java.util.Deque;
import java.util.ArrayDeque;

public class BencodeParser {

    private final ByteCursor byteCursor;

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

        String value = readAsciiUntil(END, start);

        if (value.startsWith("-0") || (value.startsWith("0") && value.length() > 1)) {
            throw new RuntimeException("Invalid Bencode Integer (leading zeros) at position: " + start);
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid Bencode Integer format at position: " + start, e);
        }
    }

    public BencodeString parseBencodedString() {
        int start = byteCursor.position();

        String lenString = readAsciiUntil(STRING_DELIMITER, start);
        int len;

        try {
            len = Integer.parseInt(lenString);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid Bencode String length at position: " + start, e);
        }

        if (len < 0 || byteCursor.remaining() < len) {
            throw new RuntimeException("Invalid Bencode String bounds at position: " + start);
        }

        return new BencodeString(byteCursor.readBytes(len));
    }

    private String readAsciiUntil(char delimiter, int startPos) {
        StringBuilder sb = new StringBuilder();
        while (byteCursor.hasRemaining()) {
            char curr = byteCursor.readChar();
            if (curr == delimiter) {
                return sb.toString();
            }
            sb.append(curr);
        }
        throw new RuntimeException("Invalid Bencode: Missing delimiter '" + delimiter + "' for structure starting at position: " + startPos);
    }


}
