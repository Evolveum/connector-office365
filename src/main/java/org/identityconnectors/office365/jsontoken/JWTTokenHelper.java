/**
 *
 * Licensed under the Microsoft Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *              http://opensource.org/licenses/MS-PL
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
**/
package org.identityconnectors.office365.jsontoken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


import javax.xml.bind.DatatypeConverter;

import org.json.JSONObject;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 * Facilitates minting a test token.
 * @author t-mislam
 *
 */
public class JWTTokenHelper {
	

    /**
     * Grant type claim
     */ 
    private static final String claimTypeGrantType = "grant_type";

    /**
     * Assertion Claim.
     */
    private static final String claimTypeAssertion = "assertion";

    /**
     * Resource Claim.
     */
    private static final String claimTypeResource = "resource";

    /**
     * Prefix for bearer tokens.
     */
    private static final String bearerTokenPrefix = "Bearer ";

	
    /**
     * Get the formatted Service Principal Name
     * @param principalName Principal Identifier
     * @param hostName Service Host Name
     * @param realm Tenant Realm.
     * @return The formatted SPN.
     */
	public static String getFormattedPrincipal(String principalName, String hostName, String realm){

		if((hostName != null) && (realm != null)){
			return String.format("%s/%s@%s", principalName, hostName, realm);
		}else if((realm == null) || (realm.isEmpty()) || (realm.trim().isEmpty())){
			return String.format("%s/%s", principalName, hostName);
		}else{
			return String.format("%s@%s", principalName, realm);
		}		
		
	}
	
	/**
	 * Returns the starting Unix Epoch Time.
	 * @return the Unix Epoch Time.
	 */
	public static Date getUnixEpochDateTime() {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.set(1970, 0, 1, 0, 0, 0);
		return calendar.getTime();
	}
	
	/**
	 * Returns the current Date Time in UTC.
	 * @return The current time in UTC.
	 */
	public static Date getCurrentDateTime() {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		return calendar.getTime();
	}

	/**
	 * Add seconds to an existing date time object.
	 * @param seconds Seconds to be added.
	 * @return The new Date Time Object.
	 */
	public static Date addSecondsToCurrentTime(int seconds) {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.add(Calendar.SECOND, seconds);
		return calendar.getTime();
		
	}


	/**
	 * Generate access token with a symmetric signing key.
	 * @param webToken JSON Web Token.
	 * @param signingKey Symmetric signing key.
	 * @return Self Signed Assertion.
	 * @throws SampleAppException If the operation is not successful.
	 */
	public static String generateAssertion(JsonWebToken webToken,
			String signingKey) throws Exception {
		
		TokenHeader tokenHeaderContract = new TokenHeader("HS256", "");
		String tokenHeader =  com.sun.org.apache.xerces.internal.impl.dv.util.Base64.encode(tokenHeaderContract.encodeToJson().getBytes());				
		String tokenBody = com.sun.org.apache.xerces.internal.impl.dv.util.Base64.encode(webToken.encodeToJson().getBytes());
		
		String rawToken = String.format("%s.%s", tokenHeader, tokenBody);		
		String signature = Base64.encode(JWTTokenHelper.signData(signingKey, rawToken));				
		String accessToken = String.format("%s.%s", rawToken, signature);		
		return accessToken;
	}

	
	/**
	 * Sign the text with the symmetric key.
	 * @param signingKey The Signing Key.
	 * @param rawToken The rawToken that needs to be signed.
	 * @return Signed byte array.
	 * @throws SampleAppException
	 */
	private static byte[] signData(String signingKey, String rawToken) throws Exception {
		SecretKeySpec secretKey = null;

		secretKey = new SecretKeySpec(com.sun.org.apache.xerces.internal.impl.dv.util.Base64.decode(signingKey), "HmacSHA256");

		Mac mac;
		byte[] signedData = null;
		
		mac = Mac.getInstance("HmacSHA256");
		mac.init(secretKey);
		mac.update(rawToken.getBytes("UTF-8"));
		signedData = mac.doFinal();
			
		return signedData;
	}


	/**
	 * Get an access token from ACS (STS).
	 * @param stsUrl ACS STS Url.
	 * @param assertion Assertion Token.
	 * @param resource ExpiresIn name.
	 * @return The OAuth access token.
	 * @throws SampleAppException If the operation can not be completed successfully.
	 */
	public static String getOAuthAccessTokenFromACS(String stsUrl,
			String assertion, String resource) throws Exception {
		
		String accessToken = "";
				
		URL url = null;
		
		String data = null;
		
		data = URLEncoder.encode(JWTTokenHelper.claimTypeGrantType, "UTF-8") + "=" + URLEncoder.encode("http://oauth.net/grant_type/jwt/1.0/bearer", "UTF-8");
		data += "&" + URLEncoder.encode(JWTTokenHelper.claimTypeAssertion, "UTF-8") + "=" + URLEncoder.encode(assertion, "UTF-8");
		data += "&" + URLEncoder.encode(JWTTokenHelper.claimTypeResource, "UTF-8") + "=" + URLEncoder.encode(resource, "UTF-8");
		
		url = new URL(stsUrl);
		
		URLConnection conn = url.openConnection();
		
		conn.setDoOutput(true);
		
		OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
		wr.write(data);
		wr.flush();
		
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		
		String line, response = "";
		
		while((line=rd.readLine()) != null){
			response += line;
		}
		
		wr.close();
		rd.close();
		
		
		accessToken = (new JSONObject(response)).optString("access_token");						
		return String.format("%s%s", JWTTokenHelper.bearerTokenPrefix, accessToken);
	}



}
