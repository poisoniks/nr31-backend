package org.nr31.backend.hibernate;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.usertype.UserType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.JacksonException;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

public class Jackson3JsonNodeType implements UserType<JsonNode> {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Override
    public int getSqlType() {
        return SqlTypes.JSON;
    }

    @Override
    public Class<JsonNode> returnedClass() {
        return JsonNode.class;
    }

    @Override
    public boolean equals(JsonNode x, JsonNode y) {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(JsonNode x) {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public JsonNode nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        String json = rs.getString(position);
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to convert String to JsonNode", e);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, JsonNode value, int index, SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            st.setObject(index, value.toString(), Types.OTHER);
        }
    }

    @Override
    public JsonNode deepCopy(JsonNode value) {
        if (value == null) {
            return null;
        }
        return value.deepCopy();
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(JsonNode value) {
        return value == null ? null : value.toString();
    }

    @Override
    public JsonNode assemble(Serializable cached, Object owner) {
        if (cached == null) {
            return null;
        }
        try {
            return MAPPER.readTree(cached.toString());
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to convert String to JsonNode", e);
        }
    }

    @Override
    public JsonNode replace(JsonNode original, JsonNode target, Object owner) {
        return deepCopy(original);
    }
}
