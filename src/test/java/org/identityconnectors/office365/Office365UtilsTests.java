package org.identityconnectors.office365;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Attempts to test the {@link Office365Utils} with the framework.
 *
 * @author Paul Heaney
 */
public class Office365UtilsTests {

    @Test
    public void testMicrosoftUIDConverstion() {
        String guid = "14c6c0c6-66fb-4c3c-a28e-a22a3e778dc4";
        String expectedEncoding = "xsDGFPtmPEyijqIqPneNxA==";
        
        String enc = Office365Utils.encodeUUIDInMicrosoftFormat(guid);
        
        Assert.assertEquals(enc, expectedEncoding);
    }
}
