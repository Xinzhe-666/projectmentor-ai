package com.xinzhe.projectmentor.retrieval.milvus;

import com.xinzhe.projectmentor.config.EmbeddingProperties;
import com.xinzhe.projectmentor.config.MilvusProperties;
import io.milvus.common.clientenum.FunctionType;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DescribeCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.collection.response.DescribeCollectionResp;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "projectmentor.milvus", name = "enabled", havingValue = "true")
public class MilvusCollectionManager {
    private final MilvusClientV2 client;
    private final MilvusProperties milvus;
    private final EmbeddingProperties embedding;

    public synchronized void ensureCollection() {
        boolean exists = client.hasCollection(HasCollectionReq.builder()
                .databaseName(milvus.getDatabase()).collectionName(milvus.getCollection()).build());
        if (!exists) {
            createCollection();
        }
        assertSchema();
        client.loadCollection(LoadCollectionReq.builder()
                .databaseName(milvus.getDatabase()).collectionName(milvus.getCollection())
                .sync(true).timeout(milvus.getTimeoutSeconds() * 1000L).build());
    }

    public void assertSchema() {
        DescribeCollectionResp response = client.describeCollection(DescribeCollectionReq.builder()
                .databaseName(milvus.getDatabase()).collectionName(milvus.getCollection()).build());
        CreateCollectionReq.FieldSchema dense = response.getCollectionSchema().getField("dense_vector");
        CreateCollectionReq.FieldSchema sparse = response.getCollectionSchema().getField("sparse_vector");
        CreateCollectionReq.FieldSchema user = response.getCollectionSchema().getField("user_id");
        if (dense == null || dense.getDataType() != DataType.FloatVector
                || !Integer.valueOf(embedding.getDimension()).equals(dense.getDimension())) {
            throw new IllegalStateException("Milvus dense_vector dimension/schema mismatch");
        }
        if (sparse == null || sparse.getDataType() != DataType.SparseFloatVector) {
            throw new IllegalStateException("Milvus sparse_vector schema mismatch");
        }
        if (user == null || !Boolean.TRUE.equals(user.getIsPartitionKey())) {
            throw new IllegalStateException("Milvus user_id partition key is missing");
        }
    }

    private void createCollection() {
        CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder()
                .enableDynamicField(false).build();
        schema.addField(field("chunk_id", DataType.VarChar, 64, true, false));
        schema.addField(field("user_id", DataType.Int64, null, false, true));
        schema.addField(field("project_id", DataType.Int64, null, false, false));
        schema.addField(field("file_id", DataType.Int64, null, false, false));
        schema.addField(field("generation", DataType.Int64, null, false, false));
        schema.addField(field("file_path", DataType.VarChar, milvus.getMaxFilePathLength(), false, false));
        schema.addField(field("language", DataType.VarChar, 32, false, false));
        schema.addField(field("chunk_type", DataType.VarChar, 32, false, false));
        schema.addField(field("symbol_name", DataType.VarChar, 256, false, false));
        schema.addField(field("start_line", DataType.Int32, null, false, false));
        schema.addField(field("end_line", DataType.Int32, null, false, false));
        schema.addField(field("content_hash", DataType.VarChar, 64, false, false));
        schema.addField(field("content", DataType.VarChar, 65535, false, false));
        schema.addField(AddFieldReq.builder().fieldName("search_text").dataType(DataType.VarChar)
                .maxLength(65535).enableAnalyzer(true).enableMatch(true)
                .analyzerParams(Map.of("tokenizer", "standard", "filter", List.of("lowercase"))).build());
        schema.addField(AddFieldReq.builder().fieldName("dense_vector").dataType(DataType.FloatVector)
                .dimension(embedding.getDimension()).build());
        schema.addField(AddFieldReq.builder().fieldName("sparse_vector").dataType(DataType.SparseFloatVector)
                .build());
        schema.addFunction(CreateCollectionReq.Function.builder()
                .name("search_text_bm25").description("BM25 over code-aware search text")
                .functionType(FunctionType.BM25).inputFieldNames(List.of("search_text"))
                .outputFieldNames(List.of("sparse_vector")).build());

        List<IndexParam> indexes = List.of(
                IndexParam.builder().fieldName("dense_vector").indexName("dense_hnsw")
                        .indexType(IndexParam.IndexType.HNSW).metricType(IndexParam.MetricType.COSINE)
                        .extraParams(Map.of("M", milvus.getHnswM(), "efConstruction", milvus.getHnswEfConstruction())).build(),
                IndexParam.builder().fieldName("sparse_vector").indexName("sparse_bm25")
                        .indexType(IndexParam.IndexType.SPARSE_INVERTED_INDEX).metricType(IndexParam.MetricType.BM25).build(),
                scalarIndex("project_id"), scalarIndex("generation"), scalarIndex("file_id")
        );
        client.createCollection(CreateCollectionReq.builder()
                .databaseName(milvus.getDatabase()).collectionName(milvus.getCollection())
                .description("PMAI v7 shared evidence-aware code chunks")
                .collectionSchema(schema).indexParams(indexes).enableDynamicField(false).build());
    }

    private AddFieldReq field(String name, DataType type, Integer maxLength,
                              boolean primary, boolean partition) {
        AddFieldReq.AddFieldReqBuilder<?> builder = AddFieldReq.builder().fieldName(name).dataType(type)
                .isPrimaryKey(primary).isPartitionKey(partition).autoID(false);
        if (maxLength != null) builder.maxLength(maxLength);
        return builder.build();
    }

    private IndexParam scalarIndex(String field) {
        return IndexParam.builder().fieldName(field).indexName(field + "_inverted")
                .indexType(IndexParam.IndexType.INVERTED).build();
    }
}
