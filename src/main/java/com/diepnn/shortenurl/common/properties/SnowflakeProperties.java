package com.diepnn.shortenurl.common.properties;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.Instant;

/**
 * Configuration properties for a Snowflake-style ID generator.
 *
 * <p>Values are bound from application configuration using the prefix {@code app.snowflake}.
 * This class encapsulates the node identity (datacenter and machine), the bit allocation for each
 * component of the Snowflake ID, and the custom epoch used to compute the time portion.</p>
 *
 * <p>Typical Snowflake layout:
 * <pre>
 *  [signBits][epochBits (time)][dataCenterBits][machineBits][sequence...]
 * </pre>
 * Only the portions managed by this properties class are documented here, the sequence
 * portion is implementation-specific.</p>
 *
 * <p>Recommendations:
 * <ul>
 *   <li>Ensure {@code datacenter} and {@code machine} values fit within the ranges
 *       implied by {@code dataCenterBits} and {@code machineBits}, respectively.</li>
 *   <li>Choose an {@code epochDate} in UTC that is fixed and consistent across all deployments.
 *       The usable time range is approximately {@code 2^{epochBits}} time units from that epoch
 *       (commonly milliseconds).</li>
 * </ul>
 * </p>
 */

@ConfigurationProperties(prefix = "app.snowflake")
@Getter
public class SnowflakeProperties {
    /**
     * Logical datacenter identifier for the node generating IDs.
     * Must be within the range {@code [0, 2^{dataCenterBits} - 1]}.
     */
    private final int datacenter;

    /**
     * Machine (or worker) identifier within the datacenter.
     * Must be within the range {@code [0, 2^{machineBits} - 1]}.
     */
    private final int machine;

    /**
     * Number of bits allocated to encode {@link #datacenter}.
     * Determines the maximum number of distinct datacenters supported.
     */
    private final int dataCenterBits;

    /**
     * Number of bits allocated to encode {@link #machine}.
     * Determines the maximum number of machines (workers) per datacenter.
     */
    private final int machineBits;

    /**
     * Number of bits allocated to encode the time component since {@link #epochDate}.
     * With millisecond precision, the usable time span is approximately {@code 2^{epochBits}} ms.
     */
    private final int epochBits;

    /**
     * Number of sign/reserved bits at the head of the Snowflake ID.
     * Typically {@code 1} to keep IDs positive in signed integer representations.
     */
    private final int signBits;

    /**
     * Number of bits allocated to encode the sequence.
     * Typically, 64 - {@link #signBits} - {@link #epochBits} - {@link #dataCenterBits} - {@link #machineBits}
     */
    private final int sequenceBits;

    /**
     * Epoch instant expressed as milliseconds since Unix epoch (UTC).
     * Derived from {@link #epochDate} for fast calculations at runtime.
     */
    private final long epoch;

    /**
     * Custom epoch (UTC) from which the time component counts forward.
     * Use a fixed, past timestamp common to all services generating IDs.
     */
    @Getter(AccessLevel.PRIVATE)
    private final Instant epochDate;

    /**
     * Constructs the properties object with all Snowflake configuration parts.
     *
     * @param datacenter     logical datacenter ID (range {@code [0, 2^{dataCenterBits} - 1]})
     * @param machine        machine/worker ID (range {@code [0, 2^{machineBits} - 1]})
     * @param dataCenterBits bit width for the datacenter field
     * @param machineBits    bit width for the machine field
     * @param epochBits      bit width for the time-since-epoch field
     * @param signBits       bit width for the sign/reserved field (often {@code 1})
     * @param epochDate      custom epoch in UTC; should be constant across all deployments
     */
    @ConstructorBinding
    public SnowflakeProperties(int datacenter, int machine, int dataCenterBits, int machineBits, int epochBits, int signBits, Instant epochDate) {
        this.datacenter = datacenter;
        this.machine = machine;
        this.dataCenterBits = dataCenterBits;
        this.machineBits = machineBits;
        this.epochBits = epochBits;
        this.signBits = signBits;
        this.epochDate = epochDate;
        this.sequenceBits = 64 - this.signBits - this.epochBits - this.dataCenterBits - this.machineBits;
        this.epoch = epochDate.toEpochMilli();
    }
}
