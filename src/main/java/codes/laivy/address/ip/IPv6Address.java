package codes.laivy.address.ip;

import codes.laivy.address.exception.IllegalAddressTypeException;
import codes.laivy.address.port.Port;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

/**
 * Represents an IPv6 address.
 * IPv6 (Internet Protocol version 6) is the most recent version of the Internet Protocol (IP), the communications protocol that provides an identification and location system for computers on networks and routes traffic across the Internet. IPv6 was developed to deal with the long-anticipated problem of IPv4 address exhaustion. IPv6 addresses are 128 bits long, compared to 32 bits in IPv4, which allows for a vastly larger number of unique IP addresses.
 * An IPv6 address is typically represented as eight groups of four hexadecimal digits, each group representing 16 bits of the address. The groups are separated by colons (e.g., "2001:0db8:85a3:0000:0000:8a2e:0370:7334"). Leading zeros in each group can be omitted, and consecutive groups of zero values can be replaced with a double colon (::). However, this substitution can only be used once in an address to avoid ambiguity.
 *
 * @author Daniel Meinicke (Laivy)
 * @since 1.0
 */
public final class IPv6Address implements IPAddress {

    // Static initializers

    private static final long serialVersionUID = 3421693018286653906L;

    /**
     * Validates if a given string is a valid IPv6 address.
     * The validation process consists of the following steps:
     * <ol>
     *   <li>If the string starts with a square bracket '[', it removes the bracket.</li>
     *   <li>Splits the string on the closing square bracket ']' to separate the address from the optional port.</li>
     *   <li>Checks that there are at most two parts (address and optional port) and at least one part (address).</li>
     *   <li>If a port is specified, validates the port using {@link Port#validate(String)}.</li>
     *   <li>Replaces the double colon (::) with the appropriate number of zero groups to normalize the address.</li>
     *   <li>Splits the normalized address on colons (:) to separate the hexadecimal groups.</li>
     *   <li>Verifies that there are exactly eight hexadecimal groups.</li>
     *   <li>Parses each hexadecimal group to an integer to ensure they are valid hexadecimal numbers.</li>
     * </ol>
     *
     * @param string the string to validate.
     * @return {@code true} if the string is a valid IPv6 address; {@code false} otherwise.
     */
    public static boolean validate(@NotNull String string) {
        // Step 1: Remove the starting square bracket if present
        if (string.startsWith("[")) {
            if (!string.contains("]")) return false;
            string = string.substring(1);
        }

        // Step 2: Split the string on the closing square bracket to separate the address from the optional port
        @NotNull String[] parts = string.split("]");
        if (parts.length > 2 || parts.length == 0) {
            return false;
        } else if (parts.length == 2) {
            // Step 3: Validate the port if present
            if (!parts[1].startsWith(":")) {
                return false;
            } else if (!Port.validate(parts[1].substring(1))) {
                return false;
            }
        }

        // Verify if it is a ipv4-mapped ipv6
        if (string.startsWith("::ffff:")) {
            if (IPv4Address.validate(string.substring(7))) {
                return true;
            }
        }

        // Consider only the address part for further validation
        string = parts[0];

        // Step 4: Replace the double colon with the appropriate number of zero groups
        int missing = 7 - string.replace("::", "").split(":").length;
        @NotNull StringBuilder zeros = new StringBuilder();
        for (int row = 0; row < missing; row++) {
            zeros.append("0:");
        }

        if (string.split("(?<=\\w):(?=\\w)").length >= 7 && string.contains("::")) {
            return false;
        }

        string = string.replace("::", ":" + zeros);
        if (string.startsWith(":")) string = "0" + string;
        if (string.endsWith(":")) string += "0";

        // Step 5: Split the normalized address on colons to separate the hexadecimal groups
        parts = string.split(":");

        // Step 6: Verify that there are exactly eight hexadecimal groups
        if (parts.length != 8) {
            return false;
        } else {
            // Step 7: Validate each hexadecimal group
            for (@NotNull String hex : parts) {
                if (hex.length() > 4) {
                    return false;
                }

                try {
                    Integer.parseInt(hex, 16);
                } catch (@NotNull NumberFormatException ignore) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Parses a given string into an {@link IPv6Address} instance.
     * The parsing process involves the following steps:
     * <ol>
     *   <li>Validates the input string using the {@link #validate(String)} method.</li>
     *   <li>Removes the starting square bracket '[' if present.</li>
     *   <li>Splits the string on the closing square bracket ']' to separate the address from the optional port.</li>
     *   <li>Normalizes the address by replacing the double colon (::) with the appropriate number of zero groups.</li>
     *   <li>Splits the normalized address on colons (:) to separate the hexadecimal groups.</li>
     *   <li>Parses each hexadecimal group from a string to a short integer.</li>
     *   <li>Creates a new {@link IPv6Address} instance with the parsed groups.</li>
     * </ol>
     *
     * @param string the string to parse.
     * @return the parsed {@link IPv6Address} instance.
     * @throws IllegalArgumentException if the string cannot be parsed as a valid IPv6 address.
     */
    public static @NotNull IPv6Address parse(@NotNull String string) throws IllegalArgumentException {
        // Step 1: Validate the input string
        if (validate(string)) {
            // Step 2: Remove the starting square bracket if present
            if (string.startsWith("[")) {
                if (!string.contains("]")) throw new IllegalArgumentException("ipv6 missing bracket close");
                string = string.substring(1);
            }

            // Step 3: Split the string on the closing square bracket to separate the address from the optional port
            @NotNull String[] parts = string.split("]");
            string = parts[0];

            // Verify if it is a ipv4-mapped ipv6
            if (string.startsWith("::ffff:")) {
                @NotNull String ipv4 = string.substring(7);

                if (IPv4Address.validate(ipv4)) {
                    @NotNull StringBuilder converter = new StringBuilder();

                    for (@NotNull String part : ipv4.split("\\.")) {
                        if (converter.length() > 0) {
                            converter.append(":");
                        }
                        converter.append(String.format("%02x", Integer.parseInt(part)));
                    }

                    string = "::ffff:" + converter;
                }
            }

            // Step 4: Parse the groups
            short[] groups = new short[8];

            // Step 5: Replace the double colon with the appropriate number of zero groups
            int missing = 7 - string.replace("::", "").split(":").length;
            @NotNull StringBuilder zeros = new StringBuilder();
            for (int row = 0; row < missing; row++) {
                zeros.append("0:");
            }
            string = string.replace("::", ":" + zeros);

            // Step 6: Split the normalized address on colons to separate the hexadecimal groups
            @NotNull String[] groupParts = string.split(":");
            for (int index = 0; index < groupParts.length; index++) {
                @NotNull String hex = groupParts[index];
                if (hex.isEmpty()) hex = "0";

                groups[index] = (short) Integer.parseInt(hex, 16);
            }

            // Step 7: Create a new IPv6Address instance
            return new IPv6Address(groups);
        } else {
            throw new IllegalArgumentException("Cannot parse '" + string + "' as a valid IPv6 address");
        }
    }

    /**
     * Converts a 128-bit integer representation to an IPv6 address.
     * <p>
     * The integer is split into eight 16-bit groups to form the IPv6 address.
     * </p>
     *
     * @param high the high 64-bit integer part of the IPv6 address.
     * @param low the low 64-bit integer part of the IPv6 address.
     * @return the {@link IPv6Address} created from the two 64-bit integers.
     */
    public static @NotNull IPv6Address fromLongs(long high, long low) {
        short[] groups = new short[8];
        for (int i = 0; i < 8; i++) {
            if (i < 4) {
                groups[i] = (short) ((high >> (48 - i * 16)) & 0xFFFF);
            } else {
                groups[i] = (short) ((low >> (48 - (i - 4) * 16)) & 0xFFFF);
            }
        }
        return new IPv6Address(groups);
    }

    // Object

    // This array will always have 8 elements
    private final short @NotNull [] groups;

    /**
     * Constructs an IPv6Address instance with the specified groups.
     *
     * @param groups an array of eight short integers representing the groups of the IPv6 address.
     * @throws IllegalArgumentException if the array does not have exactly eight elements.
     */
    public IPv6Address(short[] groups) {
        this.groups = groups;

        if (groups.length != 8) {
            throw new IllegalArgumentException("An IPv6 address must have eight hexadecimal groups");
        }
    }

    // Getters

    /**
     * Returns the groups of this IPv6 address.
     *
     * @return an array of eight short integers representing the groups of this IPv6 address.
     */
    public short[] getGroups() {
        return groups;
    }

    /**
     * Returns the raw byte values of this IPv6 address.
     *
     * @return a byte array representing the raw byte values of this IPv6 address.
     */
    @Override
    public byte @NotNull [] getBytes() {
        return getName().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns the name representation of this IPv6 address.
     * <p>
     * The name representation of an IPv6 address follows the standard IPv6 notation, which can include leading zero omission and zero abbreviation.
     * For example, the address "2001:0db8:85a3:0000:0000:8a2e:0370:7334" can be represented as "2001:db8:85a3::8a2e:370:7334".
     * </p>
     *
     * @return the name representation of this IPv6 address.
     */
    @Override
    public @NotNull String getName() {
        // Function to convert each group to its hexadecimal representation without leading zeros
        @NotNull Function<Short, String> function = new Function<Short, String>() {
            @Override
            public @NotNull String apply(@NotNull Short group) {
                @NotNull String hex = String.format("%04X", group);
                while (hex.startsWith("0")) hex = hex.substring(1);

                return hex;
            }
        };

        // Build the string representation
        @NotNull StringBuilder builder = new StringBuilder();

        for (int index = 0; index < 8; index++) { // Loop over the eight groups
            short group = getGroups()[index];

            // Generate the representation
            @NotNull String representation = function.apply(group);

            if (representation.isEmpty()) {
                representation = "0";
            }

            // Add the ":" separator
            if (builder.length() > 0) builder.append(":");
            // Add the group representation
            builder.append(representation);
        }

        return builder.toString().toLowerCase().replaceAll(":{3,}", "::");
    }

    /**
     * Returns the raw string representation of this IPv6 address.
     * <p>
     * This method provides the full, uncompressed form of the IPv6 address,
     * with each group of four hexadecimal digits separated by colons. Leading
     * zeros in each group are included.
     * </p>
     *
     * @return the raw string representation of the IPv6 address.
     */
    public @NotNull String getRawName() {
        @NotNull StringBuilder builder = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            // Format each group as four hexadecimal digits, with leading zeros
            @NotNull String group = String.format("%04X", getGroups()[i]);

            if (i > 0) {
                builder.append(":");
            }
            builder.append(group);
        }

        return builder.toString();
    }

    // Modules

    /**
     * Computes the network address for this IPv6 address given a subnet mask.
     * <p>
     * The network address is obtained by performing a bitwise AND operation between
     * the address and the subnet mask.
     * </p>
     *
     * @param subnetMask the subnet mask as an {@link IPv6Address}.
     * @return the network address as an {@link IPv6Address}.
     */
    public @NotNull IPv6Address getNetworkAddress(@NotNull IPv6Address subnetMask) {
        short[] maskGroups = subnetMask.getGroups();
        short[] networkGroups = new short[8];
        for (int i = 0; i < 8; i++) {
            networkGroups[i] = (short) (groups[i] & maskGroups[i]);
        }
        return new IPv6Address(networkGroups);
    }

    /**
     * Checks if this IPv6 address is within a given range.
     * <p>
     * The range is defined by a start address and an end address. The method verifies
     * if the current address falls within these bounds.
     * </p>
     *
     * @param start the start address of the range as an {@link IPv6Address}.
     * @param end the end address of the range as an {@link IPv6Address}.
     * @return {@code true} if this IPv6 address is within the specified range; {@code false} otherwise.
     */
    public boolean isWithinRange(@NotNull IPv6Address start, @NotNull IPv6Address end) {
        for (int i = 0; i < 8; i++) {
            if (groups[i] < start.getGroups()[i] || groups[i] > end.getGroups()[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Computes the broadcast address for this IPv6 address given a subnet mask.
     * <p>
     * The broadcast address is typically all bits set to 1 for the host portion of the address.
     * </p>
     *
     * @param subnetMask the subnet mask as an {@link IPv6Address}.
     * @return the broadcast address as an {@link IPv6Address}.
     */
    public @NotNull IPv6Address getBroadcastAddress(@NotNull IPv6Address subnetMask) {
        short[] maskGroups = subnetMask.getGroups();
        short[] broadcastGroups = new short[8];
        for (int i = 0; i < 8; i++) {
            broadcastGroups[i] = (short) (groups[i] | ~maskGroups[i] & 0xFFFF);
        }
        return new IPv6Address(broadcastGroups);
    }

    /**
     * Determines if the current IPv6 address belongs to a local network.
     * Specifically, it checks if the address is a link-local address in the
     * IPv6 range `fe80::/10`.
     * Link-local addresses are typically used for
     * communication within a single network segment or link and are not
     * routable beyond that link.
     * <p>
     * The method performs this check by examining the first 10 bits of the
     * address's first group of 16 bits (the first element in the `groups` array).
     * According to the IPv6 standard, link-local addresses have the first 10 bits
     * set to the binary value `1111 1110 10`.
     * In hexadecimal, this corresponds to
     * `0xFE80` when masked with `0xFFC0`.
     * <p>
     * This method overrides a method with the same signature in a parent class
     * or interface.
     *
     * @return {@code true} if the IPv6 address is a link-local address, {@code false} otherwise.
     */
    @Override
    public boolean isLocal() {
        short firstBlock = groups[0];

        int maskedValue = firstBlock & 0xFFC0;
        int expectedValue = 0xFE80;

        return maskedValue == expectedValue;
    }

    // Implementations

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (!(object instanceof IPv6Address)) return false;
        @NotNull IPv6Address that = (IPv6Address) object;
        return Objects.deepEquals(getGroups(), that.getGroups());
    }
    @Override
    public int hashCode() {
        return Arrays.hashCode(getGroups());
    }

    @Override
    public @NotNull String toString() {
        return getName();
    }

    /**
     * Returns a string representation of this IPv6 address with a specified port.
     *
     * @param port the port to be included in the string representation.
     * @return a string representation of this IPv6 address with the specified port.
     */
    @Override
    public @NotNull String toString(@NotNull Port port) {
        return "[" + getName() + "]:" + port;
    }

    /**
     * Returns a clone of this ipv6 address with the same bytes.
     *
     * @return the clone of this ipv6 address
     */
    @Override
    public @NotNull IPv6Address clone() {
        try {
            return (IPv6Address) super.clone();
        } catch (@NotNull CloneNotSupportedException e) {
            throw new RuntimeException("cannot clone ipv6 address", e);
        }
    }

    /**
     * Converts the IPv6 address to a full URL string, including the protocol (HTTP/HTTPS),
     * the address enclosed in square brackets (as required for IPv6), and the port if specified.
     *
     * <p>IPv6 addresses must be enclosed in square brackets in URLs to clearly delineate the address
     * from the port number, as IPv6 addresses contain colons (:) which can otherwise be confused with
     * the port delimiter.
     * This format is mandated by URL specification standards to ensure correct
     * parsing and interpretation of the address and port.
     *
     * <p>Example URL formats:
     * <ul>
     *   <li>For an IPv6 address with a specified port: {@code "https://[2001:db8::1]:8080/"}</li>
     *   <li>For an IPv6 address without a port: {@code "https://[2001:db8::1]/"}</li>
     * </ul>
     *
     * <p>The URL is constructed as follows:
     * <ul>
     *   <li>Protocol: {@code "http://"} or {@code "https://"} depending on the {@code secure} parameter.</li>
     *   <li>Address: The IPv6 address enclosed in square brackets. For example, {@code "[2001:db8::1]"}.</li>
     *   <li>Port: If a port is provided, it is appended after a colon following the address. For example, {@code ":8080"}.</li>
     *   <li>Trailing Slash: Ensures a trailing slash is present at the end of the URL if not already present.</li>
     * </ul>
     *
     * @param secure {@code true} if the URL should use HTTPS, {@code false} for HTTP
     * @param port an optional port number to be included in the URL; if {@code null}, no port is included
     * @return the full URL as a {@link String}, with the IPv6 address enclosed in square brackets
     */
    @Override
    public @NotNull String toString(boolean secure, @Nullable Port port) {
        // Generate http(s) and start bracket
        @NotNull String string = (secure ? "https://" : "http://") + '[';

        // Convert this address to string
        if (port != null) string += toString(port);
        else string += toString();

        // End bracket
        string += ']';

        // Put a slash at the end
        if (!string.endsWith("/")) string += "/";

        // Finish
        return string;
    }

    /**
     * Converts this IPv6 address to a 128-bit integer representation.
     * <p>
     * The integer representation is split into two 64-bit integers.
     * </p>
     *
     * @return an array of two long values representing the 128-bit integer.
     */
    public long @NotNull [] toLongs() {
        long[] result = new long[2];
        for (int i = 0; i < 8; i++) {
            if (i < 4) {
                result[0] |= ((long) groups[i] & 0xFFFF) << (48 - (i * 16));
            } else {
                result[1] |= ((long) groups[i] & 0xFFFF) << (48 - ((i - 4) * 16));
            }
        }
        return result;
    }

    /**
     * Determines whether the current IPv6 address is an IPv4-mapped IPv6 address.
     * <p>
     * An IPv4-mapped IPv6 address is an IPv6 address that embeds an IPv4 address.
     * The structure of an IPv4-mapped IPv6 address is:
     * <pre>
     * 0000:0000:0000:0000:0000:ffff:xxxx:xxxx
     * </pre>
     * Where "xxxx:xxxx" represents the IPv4 address.
     * <p>
     * This method checks if the last two groups of the IPv6 addresses are non-zero and equal to 0xffff,
     * which indicates that it is an IPv4-mapped address.
     * All other groups must be zero.
     *
     * @return {@code true} if the address is an IPv4-mapped IPv6 address; {@code false} otherwise.
     *
     * @since 1.2
     * @author Daniel Meinicke (Laivy)
     */
    public boolean isMapped() {
        return (groups[0] == 0 && groups[1] == 0 && groups[2] == 0 && groups[3] == 0 && groups[4] == 0 && groups[5] == (short) 0xffff);
    }

    /**
     * Converts the IPv4-mapped IPv6 address to an {@link IPv4Address}.
     * <p>
     * This method can only be called if {@link #isMapped()} returns {@code true}.
     * It extracts the
     * 32-bit IPv4 address from the last two groups of the IPv6 addresses
     * and converts it to an {@link IPv4Address}.
     * The conversion process involves extracting the high and low 16-bit segments of the address and combining
     * them into four octets.
     * <p>
     * Example:
     * <pre>
     * IPv6 Address: 0:0:0:0:0:ffff:7f00:1
     * IPv4 Address: 127.0.0.1
     * </pre>
     *
     * @return An {@link IPv4Address} instance representing the IPv4 address embedded in this IPv6 address.
     * @throws IllegalAddressTypeException if this IPv6 address is not an IPv4-mapped address.
     *
     * @since 1.2
     * @author Daniel Meinicke (Laivy)
     */
    public @NotNull IPv4Address toIPv4() throws IllegalAddressTypeException {
        if (!isMapped()) {
            throw new IllegalAddressTypeException("This IPv6 address isn't an IPv4-mapped IPv6 address");
        }

        // Get high and low
        int high = groups[7] & 0xFFFF;
        int low = groups[6] & 0xFFFF;

        // Generate octets
        int[] octets = new int[] {
                (low >> 8) & 0xFF,
                low & 0xFF,
                (high >> 8) & 0xFF,
                high & 0xFF
        };

        // Finish
        return new IPv4Address(octets);
    }

}
