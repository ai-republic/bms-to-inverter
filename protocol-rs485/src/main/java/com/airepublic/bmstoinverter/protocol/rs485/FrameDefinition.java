package com.airepublic.bmstoinverter.protocol.rs485;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class to define how a frame is structured by evaluating a definition string, e.g. SSACLOODVV (see
 * {@link FrameDefinitionPartType} for the character meanings). The pattern will be parsed to
 * produce a {@link FrameDefinitionPart}s which will then be used to parse frames from the received
 * bytes.<br/>
 * <br/>
 * 
 * IMPORTANT:<br/>
 * A frame definition string needs a {@link FrameDefinitionPartType#LENGTH} character somewhere
 * before the {@link FrameDefinitionPartType#DATA} character to determine flexible data bytes
 * length, e.g. SACLD. In that case there must be only one {@link FrameDefinitionPartType#DATA}
 * character. The length of the data bytes will be determined by the actual value in the frame bytes
 * at the position of the {@link FrameDefinitionPartType#LENGTH} part during parsing<br/>
 * If there is no {@link FrameDefinitionPartType#LENGTH} character multiple
 * {@link FrameDefinitionPartType#DATA} characters can be defined, e.g SACDDDDDDDD (8 data bytes).
 */
public class FrameDefinition implements Iterable<FrameDefinitionPart> {
    private List<FrameDefinitionPart> parts = new ArrayList<>();

    public FrameDefinition(final ArrayList<FrameDefinitionPart> parts) {
        this.parts = parts;
    }


    /**
     * Creates a {@link FrameDefinition} by parsing the specified definition string which can then
     * be used to parse a frame from received bytes.
     *
     * @param definition the definition string (see {@link FrameDefinitionPartType}
     * @return the {@link FrameDefinition}
     * @throws IllegalArgumentException thrown if then definition string contains invalid characters
     */
    public static FrameDefinition create(final String definition) throws IllegalArgumentException {
        final ArrayList<FrameDefinitionPart> parts = new ArrayList<>();
        FrameDefinitionPart current = null;

        for (int i = 0; i < definition.length(); i++) {
            // resolve the frame definition character to the type
            final FrameDefinitionPartType type = FrameDefinitionPartType.valueOf(definition.charAt(i));

            // check if the frame definition character could be resolved
            if (type == null) {
                // if not throw an exception
                throw new IllegalArgumentException("Illegal frame definition character: " + definition.charAt(i));
            }

            // if it's the same type
            if (current != null && current.getType().equals(type)) {
                // increment the number of bytes for that part
                current.setByteCount(current.getByteCount() + 1);
            } else {
                // otherwise its the first or different type
                current = new FrameDefinitionPart(type, 1);
                parts.add(current);
            }
        }

        return new FrameDefinition(parts);
    }


    /**
     * Parses the {@link FrameDefinition} and creates a frame {@link ByteBuffer} from the provided
     * bytes.
     *
     * @param definition the {@link FrameDefinition}
     * @param bytes the bytes to be parsed
     * @return the {@link ByteBuffer} containing a frame
     * @throws IndexOutOfBoundsException if the provided bytes do not contain enough bytes for a
     *         whole frame
     * @throws IllegalArgumentException if the bytes for the data length definition contains a value
     *         below 1
     */
    public ByteBuffer parse(final byte[] bytes) throws IndexOutOfBoundsException, IllegalArgumentException {
        int dataLength = -1;
        int bytesIndex = 0;

        // iterate through all parts to find the data length definition and adjust the byte count of
        // the data definition to the actual length found in the received bytes
        for (final FrameDefinitionPart part : parts) {
            switch (part.getType()) {
                // if we found the data length definition, set the data length variable according to
                // how many bytes are defined for the length
                case LENGTH: {
                    switch (part.getByteCount()) {
                        case 1:
                            dataLength = bytes[bytesIndex];
                        break;
                        case 2:
                            final byte[] shortValue = new byte[] { bytes[bytesIndex], bytes[bytesIndex + 1] };
                            dataLength = ByteBuffer.wrap(shortValue).getShort();
                        break;
                        case 4:
                            final byte[] intValue = new byte[] { bytes[bytesIndex], bytes[bytesIndex + 1], bytes[bytesIndex + 2], bytes[bytesIndex + 3] };
                            dataLength = ByteBuffer.wrap(intValue).getInt();
                        break;
                        default:
                        break;
                    }
                }
                break;

                case DATA: {
                    // check if a length part was defined
                    if (dataLength != -1) {
                        // set the previously read data length
                        part.setByteCount(dataLength);
                    }
                }
                break;
                default:
            }

            // increase the index according to the parts byte count
            bytesIndex += part.getByteCount();
        }

        if (bytesIndex <= 0) {
            throw new IndexOutOfBoundsException();
        }

        // now that the total length has been determined we can create the frame byte buffer
        return ByteBuffer.allocate(bytesIndex).put(bytes, 0, bytesIndex);
    }


    /**
     * Gets the {@link FrameDefinitionPart} at the specified index.
     *
     * @param idx the index
     * @return the {@link FrameDefinitionPart}
     */
    public FrameDefinitionPart get(final int idx) {
        return parts.get(idx);
    }


    /**
     * Gets the number of {@link FrameDefinitionPart}s this {@link FrameDefinition} consists of.
     *
     * @return the number of {@link FrameDefinitionPart}s
     */
    public int size() {
        return parts.size();
    }


    @Override
    public Iterator<FrameDefinitionPart> iterator() {
        return parts.iterator();
    }


    /**
     * Gets the {@link FrameDefinitionPart}s this {@link FrameDefinition} consists of.
     *
     * @return the {@link FrameDefinitionPart}s
     */
    public List<FrameDefinitionPart> getParts() {
        return parts;
    }
}
