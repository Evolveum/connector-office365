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

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

/**
 * ACS Token header contract
 * @author t-mislam
 *
 */
public class TokenHeader {
	
	/**
	 * Token Type.
	 */
	private String tokenType;
	
	/**
	 * Certificate Hash. 
	 */
	private String certificateHash;
	
	/**
	 * Signing Algorithm.
	 */
	private String algorithm;
	
	/**
	 * Initializes a new instance of the TokenHeader class.
	 * @param algo Signing Algorithm
	 * @param hash Certificate Hash.
	 */
	public TokenHeader(String algo, String hash){
		this.setTokenType("JWT");
		this.setAlgorithm(algo);
		this.setCertificateHash(hash);
	}

	/**
	 * @return the algorithm
	 */
	public String getAlgorithm() {
		return algorithm;
	}

	/**
	 * @param algorithm the algorithm to set
	 */
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	/**
	 * @return the certificateHash
	 */
	public String getCertificateHash() {
		return certificateHash;
	}

	/**
	 * @param certificateHash the certificateHash to set
	 */
	public void setCertificateHash(String certificateHash) {
		this.certificateHash = certificateHash;
	}

	/**
	 * @return the tokenType
	 */
	public String getTokenType() {
		return tokenType;
	}

	/**
	 * @param tokenType the tokenType to set
	 */
	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}
	
	/**
	 * Encodes the tokens into JSON Format.
	 * @return The JSON encoded token.
	 */
	public String encodeToJson(){
		Map<String, String> allClaims = new HashMap<String, String>();
		allClaims.put("alg", this.algorithm);
		allClaims.put("typ", this.tokenType);
		if((!certificateHash.equalsIgnoreCase("")) && (certificateHash != null)){
			allClaims.put("x5t", this.certificateHash);
		}
		
		return (new JSONObject(allClaims)).toString();

	}
}
