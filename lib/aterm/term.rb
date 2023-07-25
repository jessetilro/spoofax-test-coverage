require_relative 'tree_traversal'
require_relative 'pretty_printing'

module ATerm
  class Term
    include ATerm::PrettyPrinting
    include ATerm::TreeTraversal

    attr_accessor :parent, :constructor, :arguments

    def initialize(*args, constructor:)
      self.constructor = constructor
      self.arguments = args
      meet_children
    end

    def is?(name)
      name.to_s == constructor
    end

    protected

    def meet_children
      internal_terms.each do |child|
        child.parent = self
      end
    end
  end
end
