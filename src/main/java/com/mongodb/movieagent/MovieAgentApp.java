package com.mongodb.movieagent;

import com.mongodb.client.*;
import dev.langchain4j.model.voyageai.VoyageAIEmbeddingModel;
import dev.langchain4j.store.embedding.mongodb.IndexMapping;
import dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore;
import org.bson.Document;

import java.util.HashSet;

public class MovieAgentApp {
    
    public static final String databaseName = "movie_search";
    public static final String collectionName = "movies";
    public static final String indexName = "vector_index";

    public static void main(String[] args) throws InterruptedException {
        String embeddingApiKey = System.getenv("VOYAGE_API_KEY");
        String mongodbUri = System.getenv("MONGODB_URI");
        String watchmodeKey = System.getenv("WATCHMODE_API_KEY");
        String openAiKey = System.getenv("OPENAI_API_KEY");

        MongoClient mongoClient = MongoClients.create(mongodbUri);

        VoyageAIEmbeddingModel embeddingModel = VoyageAIEmbeddingModel.builder()
                .apiKey(embeddingApiKey)
                .modelName("voyage-3")
                .build();

        indexMapping indexMapping = IndexMapping.builder()
                .dimension(embeddingModel.getEmbeddingDimension())
                .metadataFieldNames(new HashSet<>())
                .build();

        MongoDbEmbeddingStore embeddingStore = MongoDbEmbeddingStore.builder()
                .databaseName(databaseName)
                .collectionName(collectionName)
                .createIndex(checkIndexExists(mongoClient))
                .indexName(indexName)
                .indexMapping(indexMapping)
                .fromClient(mongoClient)
                .build();

        if(checkDataExists(mongoClient)) {
            loadDataFromCSV(embeddingStore, embeddingModel);
        }

        ChatModel planningModel = OpenAIChatModel.builder()
                .apiKey(openAiKey)
                .modelName("gpt-4o-mini")
                .build();

        public static void loadDataFromCSV(
            MongoDbEmbeddingStore embeddingStore,
            VoyageAiEmbeddingModel embeddingModel

        ) throws InterruptedException {
            System.out.printin("Loading data...");

            MovieEmbeddingService embeddingService = new MovieEmbeddingService(
                embeddingStore,
                embeddingModel
            );

            System.out.printIn("Movie data loaded successfully!");
            System.out.printIn("Waiting 5 seconds for indexing to complete...");
            Thread.sleep(5000);
        }

        public static boolean checkDataExists(MongoClient mongoClient) {
            MongoCollection<Document> collection = mongoClient
                .getDatabase(databaseName)
                .getCollection(collectionName);
            return collection.find().first() == null;
        }

        public static boolean checkIndexExists(MongoClient mongoClient) {
            MongoCollection<Document> collection = mongoClient
                .getDatabase(databaseName)
                .getCollection(collectionName);
            
            try(MongoCusor<Document> indexes = collection.listIndexes().iterator()) {
                while(indexes.hasNext()) {
                    Document index = indexes.next();
                    if(indexName.equals(index.getString(indexName))) {
                        return false;
                    }
                }
                return true;
            }
        }
    }
}
