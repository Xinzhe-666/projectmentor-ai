package com.xinzhe.projectmentor.retrieval.milvus;

import com.xinzhe.projectmentor.config.EmbeddingProperties;
import com.xinzhe.projectmentor.config.MilvusProperties;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.database.request.CreateDatabaseReq;
import io.milvus.v2.service.rbac.request.CreateRoleReq;
import io.milvus.v2.service.rbac.request.CreateUserReq;
import io.milvus.v2.service.rbac.request.GrantPrivilegeReqV2;
import io.milvus.v2.service.rbac.request.GrantRoleReq;
import io.milvus.v2.service.rbac.request.UpdatePasswordReq;

import java.util.List;
import java.util.Map;

/** One-time controlled bootstrap. It is never called by Spring application startup. */
public final class MilvusRbacInitializer {
    private MilvusRbacInitializer() {}

    public static void main(String[] args) throws Exception {
        Map<String, String> env = System.getenv();
        String uri = required(env, "MILVUS_ADMIN_URI");
        String currentRoot = required(env, "MILVUS_ROOT_CURRENT_PASSWORD");
        String newRoot = required(env, "MILVUS_ROOT_NEW_PASSWORD");
        String appUser = required(env, "MILVUS_APP_USERNAME");
        String appPassword = required(env, "MILVUS_APP_PASSWORD");
        String database = env.getOrDefault("MILVUS_DATABASE", "pmai");
        String collection = env.getOrDefault("MILVUS_COLLECTION", "pmai_code_chunks_v1");
        int dimension = Integer.parseInt(env.getOrDefault("EMBEDDING_DIMENSION", "1536"));
        String role = "pmai_app_role";

        MilvusClientV2 client = new MilvusClientV2(ConnectConfig.builder()
                .uri(uri).token("root:" + currentRoot).connectTimeoutMs(10000).rpcDeadlineMs(30000).build());
        try {
            if (!client.listDatabases().getDatabaseNames().contains(database)) {
                client.createDatabase(CreateDatabaseReq.builder().databaseName(database).build());
            }
            client.useDatabase(database);
            MilvusProperties milvus = new MilvusProperties();
            milvus.setEnabled(true); milvus.setUri(uri); milvus.setDatabase(database); milvus.setCollection(collection);
            EmbeddingProperties embedding = new EmbeddingProperties();
            embedding.setEnabled(true); embedding.setDimension(dimension);
            new MilvusCollectionManager(client, milvus, embedding).ensureCollection();

            if (!client.listRoles().contains(role)) {
                client.createRole(CreateRoleReq.builder().roleName(role).description("PMAI application role").build());
            }
            if (!client.listUsers().contains(appUser)) {
                client.createUser(CreateUserReq.builder().userName(appUser).password(appPassword)
                        .description("PMAI backend application user").build());
            }
            for (String privilege : List.of("CollectionReadWrite", "DescribeCollection", "Load", "GetLoadState")) {
                client.grantPrivilegeV2(GrantPrivilegeReqV2.builder().roleName(role).privilege(privilege)
                        .dbName(database).collectionName(collection).build());
            }
            client.grantRole(GrantRoleReq.builder().userName(appUser).roleName(role).build());
            if (!currentRoot.equals(newRoot)) {
                client.updatePassword(UpdatePasswordReq.builder().userName("root").password(currentRoot)
                        .newPassword(newRoot).resetConnection(true).build());
            }
            System.out.println("Milvus RBAC initialization completed; application user and collection are ready.");
        } finally {
            client.close();
        }
    }

    private static String required(Map<String, String> env, String key) {
        String value = env.get(key);
        if (value == null || value.isBlank() || value.startsWith("replace-with")) {
            throw new IllegalArgumentException(key + " must be set to a non-placeholder value");
        }
        return value;
    }
}
