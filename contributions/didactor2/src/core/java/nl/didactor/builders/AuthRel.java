package nl.didactor.builders;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.module.corebuilders.InsRel;

/**
 * @author Johannes Verelst &lt;johannes.verelst@eo.nl&gt;
 */
public class AuthRel extends InsRel {
    private static Logger log = Logging.getLoggerInstance(AuthRel.class.getName());

    /**
     * Initialize the builder
     * @return Boolean indication whether or not the call succeeded.
     */
    public boolean init() {
        return super.init();
    }
}
