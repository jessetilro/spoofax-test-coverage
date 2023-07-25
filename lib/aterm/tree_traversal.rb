module ATerm
  module TreeTraversal
    def root
      return self if parent.nil?

      @root ||= nearest_ancestor { |term| term.parent.nil? }
    end

    def internal_terms
      children.reject { |term| leaf?(term) }
    end

    def leaf_terms
      children.select { |term| leaf?(term) }
    end

    def leaf?(term)
      (!term.is_a?(Array) || term.empty?) &&
        (!term.respond_to?(:children?) || term.children.empty?)
    end

    def primitive?(term)
      !term.is_a?(ATerm::Term)
    end

    def children
      arguments.flatten
    end

    def nearest_ancestor(type = nil, &condition)
      condition = ->(x) { x.is?(type) } unless type.nil?
      condition ||= ->(_) { true }
      ancestor = self
      while (ancestor = ancestor.parent) && !ancestor.nil? && !condition[ancestor]
      end
      ancestor
    end

    def nearest_descendent(type = nil, &condition)
      condition = ->(x) { x.is?(type) } unless type.nil?
      bfs do |term|
        return term if condition[term]
      end
    end

    def dfs(&visit)
      visit[self]
      internal_terms.each do |term|
        term.dfs(&visit)
      end
      nil
    end

    def bfs(&visit)
      queue = [self]
      until queue.empty?
        term = queue.shift
        visit[term]
        queue.concat(term.internal_terms)
      end
      nil
    end
  end
end
