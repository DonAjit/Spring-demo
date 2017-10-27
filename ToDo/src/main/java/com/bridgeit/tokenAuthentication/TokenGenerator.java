package com.bridgeit.tokenAuthentication;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.bridgeit.entity.Token;

@Service("/tokenService")
public class TokenGenerator {

	Config config;

	RedissonClient redisson;

	RMapCache<String, Token> map;

	Logger logger = LoggerFactory.getLogger(TokenGenerator.class);

	@PostConstruct
	public void initializeRedis() throws IOException {

		config = new Config();

		config.useSingleServer().setAddress("127.0.0.1:6379");

		config = Config.fromJSON("{\n" + "   \"singleServerConfig\":{\n" + "      \"idleConnectionTimeout\":10000,\n"
				+ "      \"pingTimeout\":1000,\n" + "      \"connectTimeout\":10000,\n" + "      \"timeout\":3000,\n"
				+ "      \"retryAttempts\":3,\n" + "      \"retryInterval\":1500,\n"
				+ "      \"reconnectionTimeout\":3000,\n" + "      \"failedAttempts\":3,\n"
				+ "      \"password\":null,\n" + "      \"subscriptionsPerConnection\":5,\n"
				+ "      \"clientName\":null,\n" + "      \"address\": \"redis://127.0.0.1:6379\",\n"
				+ "      \"subscriptionConnectionMinimumIdleSize\":1,\n"
				+ "      \"subscriptionConnectionPoolSize\":50,\n" + "      \"connectionMinimumIdleSize\":10,\n"
				+ "      \"connectionPoolSize\":64,\n" + "      \"database\":0,\n" + "      \"dnsMonitoring\":false,\n"
				+ "      \"dnsMonitoringInterval\":5000\n" + "   },\n" + "   \"threads\":0,\n"
				+ "   \"nettyThreads\":0,\n" + "   \"codec\":null,\n" + "   \"useLinuxNativeEpoll\":false\n" + "}");

		RedissonClient redisson = Redisson.create(config);

		map = redisson.getMapCache("TestMap");

	}

	@PreDestroy
	public void shutdownRedis() {
		redisson.shutdown();
	}

	public Token generateTokenAndPushIntoRedis(Integer userId, String tokenType) {
		UUID uuid = UUID.randomUUID();
		String randomUUID = uuid.toString().replaceAll("-", "");
		System.out.println("Random UUID is: " + randomUUID);
		Token token = new Token();
		token.setUserId(userId);
		token.setTokenType(tokenType);
		token.setTokenValue(randomUUID);

		switch (tokenType) {
		case "accesstoken":
			map.put(randomUUID, token, 24, TimeUnit.HOURS);
			System.out.println("Storing access Token as: " + map.get(randomUUID));
			break;
		case "refreshtoken":
			map.put(randomUUID, token, 24, TimeUnit.HOURS);
			System.out.println("Storing refresh Token as:" + map.get(randomUUID));
			break;
		case "forgottoken":
			map.put(randomUUID, token, 24, TimeUnit.HOURS);
			break;
		default:
			logger.info("Invalid Choice");
		}

		// saving same token for userId into REDIS
		// push into REDIS
		// no need to push into MySQL DB anymore

		System.out.println(tokenType + randomUUID + " Set successfully for user: " + userId);

		return token;

	}

	public boolean verifyUserToken(Integer userId, String userTokenId, String tokenType) {

		// check if tokenUUID exists

		System.out.println(tokenType + " Redis token is: " + map.get(userTokenId));
		System.out.println("User token is: " + userTokenId);

		// verify token value, token user, and token type

		if (map.containsKey(userTokenId) && map.get(userTokenId).getUserId().compareTo(userId) == 0
				&& map.get(userTokenId).getTokenType().equalsIgnoreCase(tokenType)) {
			System.out.println(tokenType + " authentication success");
			return true;
		}
		System.out.println(tokenType + " authentication failed");
		return false;

	}
}
