package com.krickert.search.indexer.grpc;

import com.github.javafaker.Faker;
import com.krickert.search.service.*;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;

import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;

import static java.lang.reflect.Modifier.isPublic;
import static org.mockito.Mockito.*;

@Factory
public class ChunkServiceMock {

    private final Faker faker = new Faker();
    private final Random random = new SecureRandom();
    private final List<Method> fakerMethods = new ArrayList<>();
    private final Map<Method, Object> methodCategoryMap = new HashMap<>();
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("[^a-zA-Z0-9\\s]");

    public ChunkServiceMock() {
        List<Object> fakerCategories = List.of(
                faker.ancient(), faker.app(), faker.artist(), faker.avatar(), faker.aviation(), faker.lorem(),
                faker.music(), faker.name(), faker.number(), faker.internet(), faker.phoneNumber(), faker.pokemon(),
                faker.address(), faker.business(), faker.book(), faker.chuckNorris(), faker.color(), faker.commerce(),
                faker.country(), faker.currency(), faker.company(), faker.crypto(), faker.idNumber(), faker.hacker(),
                faker.options(), faker.code(), faker.finance(), faker.food(), faker.gameOfThrones(), faker.date(),
                faker.demographic(), faker.dog(), faker.educator(), faker.elderScrolls(), faker.shakespeare(),
                faker.slackEmoji(), faker.space(), faker.superhero(), faker.bool(), faker.team(), faker.beer(),
                faker.university(), faker.cat(), faker.file(), faker.stock(), faker.lordOfTheRings(), faker.zelda(),
                faker.harryPotter(), faker.rockBand(), faker.esports(), faker.friends(), faker.hipster(), faker.job(),
                faker.twinPeaks(), faker.rickAndMorty(), faker.yoda(), faker.matz(), faker.witcher(), faker.dragonBall(),
                faker.funnyName(), faker.hitchhikersGuideToTheGalaxy(), faker.hobbit(), faker.howIMetYourMother(),
                faker.leagueOfLegends(), faker.overwatch(), faker.robin(), faker.starTrek(), faker.weather(),
                faker.lebowski(), faker.medical(), faker.animal(), faker.backToTheFuture(), faker.princessBride(),
                faker.buffy(), faker.relationships(), faker.nation(), faker.dune(), faker.aquaTeenHungerForce(),
                faker.programmingLanguage()
        );

        for (Object category : fakerCategories) {
            for (Method method : category.getClass().getDeclaredMethods()) {
                if (isPublic(method.getModifiers()) && method.getReturnType().equals(String.class) && method.getParameterCount() == 0) {
                    fakerMethods.add(method);
                    methodCategoryMap.put(method, category);
                }
            }
        }
    }

    @Bean
    public ChunkServiceGrpc.ChunkServiceBlockingStub createMock() {
        ChunkServiceGrpc.ChunkServiceBlockingStub mockStub = mock(ChunkServiceGrpc.ChunkServiceBlockingStub.class);

        // Mock chunk method
        when(mockStub.chunk(any(ChunkRequest.class))).thenAnswer(invocation -> {
            ChunkRequest request = invocation.getArgument(0);
            int chunkLength = request.getOptions().getLength();
            int overlap = request.getOptions().getOverlap();

            List<String> chunks = new ArrayList<>();
            int numberOfChunks = random.nextInt(100) + 1;  // Random number from 1 to 100
            // Generate a pool of random words sufficient for all chunks
            List<String> wordsPool = generateWordsPool(chunkLength, overlap, numberOfChunks);

            for (int i = 0; i < numberOfChunks; i++) {
                int startIndex = i * (chunkLength - overlap);
                List<String> chunkWords = wordsPool.subList(startIndex, startIndex + chunkLength);
                chunks.add(String.join(" ", chunkWords));
            }

            return ChunkReply.newBuilder().addAllChunks(chunks).build();
        });

        // Mock check (health check) method
        HealthCheckReply healthCheckReply = HealthCheckReply.newBuilder()
                .setStatus("RUNNING").setTimerRunning(7777777).setServerName("localhost-mock")
                .build();
        when(mockStub.check(any(HealthCheckRequest.class))).thenReturn(healthCheckReply);

        return mockStub;
    }

    private List<String> generateWordsPool(int chunkLength, int overlap, int numberOfChunks) throws Exception {
        int totalWordsNeeded = (chunkLength * numberOfChunks) - (overlap * (numberOfChunks - 1));
        List<String> words = new ArrayList<>();

        while (words.size() < totalWordsNeeded) {
            String wordGroup = generateRandomWords();
            StringTokenizer tokenizer = new StringTokenizer(SYMBOL_PATTERN.matcher(wordGroup).replaceAll(""));
            while (tokenizer.hasMoreTokens()) {
                words.add(tokenizer.nextToken());
            }
        }

        return words;
    }

    private String generateRandomWords() throws Exception {
        Method method = fakerMethods.get(random.nextInt(fakerMethods.size()));
        Object category = methodCategoryMap.get(method);
        return (String) method.invoke(category);
    }
}