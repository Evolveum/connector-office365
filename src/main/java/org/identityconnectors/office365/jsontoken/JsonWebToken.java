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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;


/**
 * Implements the data contract for the Json Web Security Token.
 * @author t-mislam
 *
 */
public class JsonWebToken {
	@SuppressWarnings("unused")
	
	/**
	 * Start of the Date Time.
	 */
	private Date unixEpochDateTime;
	
	/**
	 * Claim issuer.
	 */
	private String issuer;
	
	/**
	 * Audience of the token.
	 */
	private String audience;
	
	/**
	 * The token is not valid before time.
	 */
	private Date notBeforeDate;
	
	/**
	 * The expirationDate.
	 */
	private Date expirationDate;
	
	/**
	 * This holds the other claims.
	 */
	private HashMap<String, String> otherClaims;

	/**
	 * Initializes a new instance of the JsonWebToken class, initializes claims from raw values.
	 * @param issuerPrincipalId Service Principal Id of the issuer.
	 * @param tenantRealm Realm or context id of the tenant.
	 * @param audienceHostName Audience host name.
	 * @param audiencePrincipalId PrincipalId of the protected resource.
	 * @param nbfTime DateTime of the NotBefore claim.
	 */
	public JsonWebToken( String issuerPrincipalId, String tenantRealm, String audienceHostName, String audiencePrincipalId, Date nbfTime, int validitySeconds){
		this.otherClaims = new HashMap<String, String>();
		this.issuer = JWTTokenHelper.getFormattedPrincipal(issuerPrincipalId, null, tenantRealm);
		this.audience = JWTTokenHelper.getFormattedPrincipal(audiencePrincipalId, audienceHostName, tenantRealm);
		this.notBeforeDate = nbfTime;
		this.expirationDate = JWTTokenHelper.addSecondsToCurrentTime(validitySeconds);
		this.unixEpochDateTime = JWTTokenHelper.getUnixEpochDateTime();
	}



	/**
	 * @return the issuer
	 */
	public String getIssuer() {
		return issuer;
	}



	/**
	 * @param issuer the issuer to set
	 */
	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}



	/**
	 * @return the otherClaims
	 */
	public HashMap<String, String> getOtherClaims() {
		return otherClaims;
	}



	/**
	 * @param otherClaims the otherClaims to set
	 */
	public void setOtherClaims(HashMap<String, String> otherClaims) {
		this.otherClaims = otherClaims;
	}


	/**
	 * @return the notBeforeDate
	 */
	public Date getNotBeforeDate() {
		return notBeforeDate;
	}



	/**
	 * @param notBeforeDate the notBeforeDate to set
	 */
	public void setNotBeforeDate(Date notBeforeDate) {
		this.notBeforeDate = notBeforeDate;
	}



	/**
	 * @return the audience
	 */
	public String getAudience() {
		return audience;
	}



	/**
	 * @param audience the audience to set
	 */
	public void setAudience(String audience) {
		this.audience = audience;
	}


	/**
	 * Encodes the tokens into JSON Format.
	 * @return OtherClaims encoded in JSON.
	 */
	public String encodeToJson() {
		Map<String, String> allClaims = new HashMap<String, String>();
		
		allClaims.put("aud", this.audience);
		allClaims.put("iss", this.issuer);
		
		String totalSeconds = "" +  (this.notBeforeDate.getTime()/1000);		
		allClaims.put("nbf", totalSeconds);
		
		String expSeconds = "" + (this.expirationDate.getTime() / 1000);
		allClaims.put("exp", expSeconds);
		
		
				
		return (new JSONObject(allClaims)).toString();
	}

}
