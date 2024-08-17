package codes.laivy.address.http;

import codes.laivy.address.Address;
import codes.laivy.address.domain.Domain;
import codes.laivy.address.ip.IPAddress;
import codes.laivy.address.ip.IPv4Address;
import codes.laivy.address.ip.IPv6Address;
import codes.laivy.address.port.Port;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code HttpAddress} interface represents an address that can be used within the context
 * of HTTP URLs. This interface extends the {@link Address} interface, indicating that any
 * implementing class represents an addressable entity in network communications. Specifically,
 * it is designed for use with HTTP protocols, enabling integration with web resources and services.
 *
 * <p>The primary purpose of this interface is to provide a common type for different address
 * representations that can be utilized in HTTP contexts, such as in the construction of URLs.
 * Implementations of this interface, like {@link Domain} and {@link IPAddress}, provide concrete
 * details on how these addresses can be formatted and used in HTTP requests.</p>
 *
 * <p>For instance, a {@link Domain} represents a domain name (such as "example.com"), which can be
 * used as a part of a URL to access web resources. Similarly, an {@link IPAddress} represents an
 * IP address (such as "192.168.1.1" for IPv4 or "2001:0db8:85a3:0000:0000:8a2e:0370:7334" for IPv6),
 * which can also be used in URLs to direct traffic to specific servers or services.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * HttpAddress address1 = new Domain("example.com");
 * HttpAddress address2 = new IPv4Address("192.168.1.1");
 *
 * // Use HttpAddress in HTTP URL construction
 * URL url1 = new URL("http://" + address1 + "/path");
 * URL url2 = new URL("http://" + address2 + "/path");
 * }
 * </pre>
 *
 * <p>Note: This interface is intended to be implemented by classes that represent addressable
 * entities in HTTP protocols. It does not define specific methods, as the format and behavior of
 * addresses can vary significantly.</p>
 *
 * @see Address
 * @see Domain
 * @see IPAddress
 */
public interface HttpAddress extends Address {

    // Static initializers

    /**
     * Validates whether a given string represents a valid http address.
     *
     * @param string the string to validate.
     * @return {@code true} if the string is a valid address; {@code false} otherwise.
     */
    static boolean validate(@NotNull String string) {
        @Nullable Class<? extends Address> type = Address.getType(string);
        return type != null && HttpAddress.class.isAssignableFrom(type);
    }

    /**
     * Parses a string into an {@link HttpAddress} instance, which may be an {@link IPv4Address},
     * {@link IPv6Address}, or {@link Domain}.
     *
     * @param string the string to parse.
     * @return the parsed {@link Address} instance.
     * @throws IllegalArgumentException if the string cannot be parsed as a valid address.
     */
    static @NotNull HttpAddress parse(@NotNull String string) {
        @Nullable Class<? extends Address> type = Address.getType(string);

        if (type != null) {
            if (type == Domain.class) {
                return Domain.parse(string);
            } else if (type == IPv4Address.class) {
                return IPv4Address.parse(string);
            } else if (type == IPv6Address.class) {
                return IPv6Address.parse(string);
            } else {
                throw new UnsupportedOperationException("Unsupported address class: '" + type.getName() + "'");
            }
        } else {
            throw new IllegalArgumentException("Invalid address: '" + string + "'");
        }
    }

    // Object

    /**
     * Generates the full URL of the host, including the protocol (HTTP/HTTPS),
     * the address, and the port if specified.
     *
     * @param secure {@code true} if the URL should use HTTPS, {@code false} for HTTP
     * @param port the port of the URL (may be null)
     * @return the full URL as a {@link String}
     */
    default @NotNull String toString(boolean secure, @Nullable Port port) {
        // Generate http(s)
        @NotNull String string = (secure ? "https://" : "http://");

        // Convert this address to string
        if (port != null) string += toString(port);
        else string += toString();

        // Put a slash at the end
        if (!string.endsWith("/")) string += "/";

        // Finish
        return string;
    }

}