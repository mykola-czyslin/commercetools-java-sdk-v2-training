package handson.graphql;

import io.aexp.nodes.graphql.annotations.GraphQLArgument;
import io.aexp.nodes.graphql.annotations.GraphQLProperty;

@GraphQLProperty(name = "products", arguments = { @GraphQLArgument(name = "limit", type = "Integer"), @GraphQLArgument(name = "sort", type = "String") })
public class ProductData extends Products {
}
