package scenarios;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

@SuppressWarnings("nls")
public class SecurityTestCase
{

    /**
     * Issue: If historical quotes start only after the purchase (or delivery)
     * of a security, the security is valued at 0 (no quote available) and the
     * performance would go crazy to -100% (as reported in the forum). This
     * scenario makes sure that earliest available historical quote is used.
     */
    @Test
    public void testSecurityPerformanceWithMissingHistoricalQuotes() throws IOException
    {
        Client client = ClientFactory.load(SecurityTestCase.class
                        .getResourceAsStream("security_performance_with_missing_historical_quotes.xml"),
                        new NullProgressMonitor());

        Security security = client.getSecurities().get(0);
        PortfolioTransaction delivery = client.getPortfolios().get(0).getTransactions().get(0);

        assertThat("delivery transaction must be before earliest historical quote", delivery.getDate(),
                        lessThan(security.getPrices().get(0).getTime()));

        ReportingPeriod period = new ReportingPeriod.FromXtoY(Dates.date("2013-12-04"), Dates.date("2014-12-04"));
        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, period);

        SecurityPerformanceRecord record = snapshot.getRecords().get(0);

        assertThat(record.getSecurity().getName(), is("Basf SE"));
        assertThat(record.getTrueTimeWeightedRateOfReturn(), closeTo(-0.0594, 0.0001));
        assertThat(record.getIrr(), closeTo(-0.0643, 0.0001));

        // actually, in this simple scenario (no cash transfers involved), the
        // ttwror is easy to calculate:

        double endvalue = delivery.getShares() * security.getSecurityPrice(Dates.date("2014-12-04")).getValue()
                        / Values.Share.divider();

        assertThat(record.getTrueTimeWeightedRateOfReturn(), closeTo((endvalue / delivery.getAmount()) - 1, 0.0001));
    }
}
