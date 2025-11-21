package theater;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/**
 * This class generates a statement for a given invoice of performances.
 */
public class StatementPrinter {

    private final Invoice invoice;
    private final Map<String, Play> plays;

    /**
     * Creates a new StatementPrinter.
     *
     * @param invoice the invoice to print
     * @param plays   the mapping from play id to play information
     */
    public StatementPrinter(Invoice invoice, Map<String, Play> plays) {
        this.invoice = invoice;
        this.plays = plays;
    }

    /**
     * Returns a formatted statement for the invoice associated with this printer.
     *
     * @return the formatted statement string
     * @throws RuntimeException if one of the play types is not known
     */
    public String statement() {
        final StringBuilder result = new StringBuilder(
                "Statement for " + invoice.getCustomer() + System.lineSeparator()
        );

        // first pass: print each performance line
        for (Performance performance : invoice.getPerformances()) {
            result.append(String.format("  %s: %s (%s seats)%n",
                    getPlay(performance).getName(),
                    usd(getAmount(performance)),
                    performance.getAudience()));
        }

        // second pass: compute total volume credits
        final int volumeCredits = getTotalVolumeCredits();

        // third pass: compute total amount
        final int totalAmount = getTotalAmount();

        result.append(String.format("Amount owed is %s%n", usd(totalAmount)));
        result.append(String.format("You earned %s credits%n", volumeCredits));
        return result.toString();
    }

    /**
     * Returns the play associated with the given performance.
     *
     * @param performance the performance whose play is needed
     * @return the play for the performance
     */
    private Play getPlay(Performance performance) {
        return plays.get(performance.getPlayID());
    }

    /**
     * Computes the amount owed for a single performance in cents.
     *
     * @param performance the performance being billed
     * @return the amount in cents
     * @throws RuntimeException if the play type is unknown
     */
    private int getAmount(Performance performance) {
        final Play play = getPlay(performance);
        int result;
        switch (play.getType()) {
            case "tragedy":
                result = Constants.TRAGEDY_BASE_AMOUNT;
                if (performance.getAudience() > Constants.TRAGEDY_AUDIENCE_THRESHOLD) {
                    result += Constants.TRAGEDY_OVER_BASE_CAPACITY_PER_PERSON
                            * (performance.getAudience() - Constants.TRAGEDY_AUDIENCE_THRESHOLD);
                }
                break;
            case "comedy":
                result = Constants.COMEDY_BASE_AMOUNT;
                if (performance.getAudience() > Constants.COMEDY_AUDIENCE_THRESHOLD) {
                    result += Constants.COMEDY_OVER_BASE_CAPACITY_AMOUNT
                            + Constants.COMEDY_OVER_BASE_CAPACITY_PER_PERSON
                            * (performance.getAudience() - Constants.COMEDY_AUDIENCE_THRESHOLD);
                }
                result += Constants.COMEDY_AMOUNT_PER_AUDIENCE * performance.getAudience();
                break;
            default:
                throw new RuntimeException(String.format("unknown type: %s", play.getType()));
        }
        return result;
    }

    /**
     * Computes the volume credits for a single performance.
     *
     * @param performance the performance being evaluated
     * @return the volume credits earned for this performance
     */
    private int getVolumeCredits(Performance performance) {
        int result = Math.max(
                performance.getAudience() - Constants.BASE_VOLUME_CREDIT_THRESHOLD, 0);
        if ("comedy".equals(getPlay(performance).getType())) {
            result += performance.getAudience() / Constants.COMEDY_EXTRA_VOLUME_FACTOR;
        }
        return result;
    }

    /**
     * Computes the total volume credits for all performances in the invoice.
     *
     * @return the total volume credits
     */
    private int getTotalVolumeCredits() {
        int result = 0;
        for (Performance performance : invoice.getPerformances()) {
            result += getVolumeCredits(performance);
        }
        return result;
    }

    /**
     * Computes the total amount owed for all performances in the invoice, in cents.
     *
     * @return the total amount in cents
     */
    private int getTotalAmount() {
        int result = 0;
        for (Performance performance : invoice.getPerformances()) {
            result += getAmount(performance);
        }
        return result;
    }

    /**
     * Formats a monetary amount in cents as a US dollar string.
     *
     * @param amount the amount in cents
     * @return the formatted US dollar string
     */
    private String usd(int amount) {
        return NumberFormat.getCurrencyInstance(Locale.US)
                .format(amount / (double) Constants.PERCENT_FACTOR);
    }
}

