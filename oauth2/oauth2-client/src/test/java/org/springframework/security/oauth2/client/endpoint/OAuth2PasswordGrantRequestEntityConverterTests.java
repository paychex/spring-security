/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.oauth2.client.endpoint;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.TestClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.ClientAssertionParameterNames;
import org.springframework.security.oauth2.core.endpoint.ClientAssertionParameterValues;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

/**
 * Tests for {@link OAuth2PasswordGrantRequestEntityConverter}.
 *
 * @author Joe Grandja
 */
public class OAuth2PasswordGrantRequestEntityConverterTests {
	private OAuth2PasswordGrantRequestEntityConverter converter = new OAuth2PasswordGrantRequestEntityConverter();
	private OAuth2PasswordGrantRequest passwordGrantRequest;

	@Before
	public void setup() {
		ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration()
				.authorizationGrantType(AuthorizationGrantType.PASSWORD)
				.scope("read", "write")
				.build();
		this.passwordGrantRequest = new OAuth2PasswordGrantRequest(clientRegistration, "user1", "password");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void convertWhenGrantRequestValidThenConverts() {
		RequestEntity<?> requestEntity = this.converter.convert(this.passwordGrantRequest);

		ClientRegistration clientRegistration = this.passwordGrantRequest.getClientRegistration();

		assertThat(requestEntity.getMethod()).isEqualTo(HttpMethod.POST);
		assertThat(requestEntity.getUrl().toASCIIString()).isEqualTo(
				clientRegistration.getProviderDetails().getTokenUri());

		HttpHeaders headers = requestEntity.getHeaders();
		assertThat(headers.getAccept()).contains(MediaType.APPLICATION_JSON_UTF8);
		assertThat(headers.getContentType()).isEqualTo(
				MediaType.valueOf(APPLICATION_FORM_URLENCODED_VALUE + ";charset=UTF-8"));
		assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).startsWith("Basic ");

		MultiValueMap<String, String> formParameters = (MultiValueMap<String, String>) requestEntity.getBody();
		assertThat(formParameters.getFirst(OAuth2ParameterNames.GRANT_TYPE)).isEqualTo(
				AuthorizationGrantType.PASSWORD.getValue());
		assertThat(formParameters.getFirst(OAuth2ParameterNames.USERNAME)).isEqualTo("user1");
		assertThat(formParameters.getFirst(OAuth2ParameterNames.PASSWORD)).isEqualTo("password");
		assertThat(formParameters.getFirst(OAuth2ParameterNames.SCOPE)).isEqualTo("read write");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void convertWhenGrantRequestJWTSecretValidThenConverts() {


		ClientRegistration clientRegistration = this.from(this.passwordGrantRequest.getClientRegistration())
				.clientAuthenticationMethod(ClientAuthenticationMethod.SECRET_JWT.SECRET_JWT)
				.clientSecret("2ae2135579004d5d87ae8241603c0a5c")
				.build();
		OAuth2PasswordGrantRequest passwordGrantRequest = new OAuth2PasswordGrantRequest(clientRegistration, "user1", "password");

		RequestEntity<?> requestEntity = this.converter.convert(passwordGrantRequest);
		assertThat(requestEntity.getMethod()).isEqualTo(HttpMethod.POST);
		assertThat(requestEntity.getUrl().toASCIIString()).isEqualTo(
				clientRegistration.getProviderDetails().getTokenUri());

		HttpHeaders headers = requestEntity.getHeaders();
		assertThat(headers.getAccept()).contains(MediaType.APPLICATION_JSON_UTF8);
		assertThat(headers.getContentType()).isEqualTo(
				MediaType.valueOf(APPLICATION_FORM_URLENCODED_VALUE + ";charset=UTF-8"));

		MultiValueMap<String, String> formParameters = (MultiValueMap<String, String>) requestEntity.getBody();
		assertThat(formParameters.getFirst(OAuth2ParameterNames.GRANT_TYPE)).isEqualTo(
				AuthorizationGrantType.PASSWORD.getValue());
		assertThat(formParameters.getFirst(OAuth2ParameterNames.USERNAME)).isEqualTo("user1");
		assertThat(formParameters.getFirst(OAuth2ParameterNames.PASSWORD)).isEqualTo("password");
		assertThat(formParameters.getFirst(OAuth2ParameterNames.SCOPE)).isEqualTo("read write");
		assertThat(formParameters.getFirst(ClientAssertionParameterNames.CLIENT_ASSERTION)).isNotEmpty();
		assertThat(formParameters.getFirst(ClientAssertionParameterNames.CLIENT_ASSERTION_TYPE)).isEqualTo(
				ClientAssertionParameterValues.CLIENT_ASSERTION_TYPE_JWT_BEARER);
	}


	private ClientRegistration.Builder from(ClientRegistration registration) {
		return ClientRegistration.withRegistrationId(registration.getRegistrationId())
				.clientId(registration.getClientId())
				.clientSecret(registration.getClientSecret())
				.clientAuthenticationMethod(registration.getClientAuthenticationMethod())
				.authorizationGrantType(registration.getAuthorizationGrantType())
				.redirectUriTemplate(registration.getRedirectUriTemplate())
				.scope(registration.getScopes())
				.authorizationUri(registration.getProviderDetails().getAuthorizationUri())
				.tokenUri(registration.getProviderDetails().getTokenUri())
				.userInfoUri(registration.getProviderDetails().getUserInfoEndpoint().getUri())
				.userNameAttributeName(registration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName())
				.clientName(registration.getClientName());
	}
}
