require_relative 'term'

class Object
  class << self
    def const_missing(name)
      ATerm::Term.new(constructor: name.to_s)
    end
  end
end

module ATerm
  class DSL
    attr_accessor :instance

    private_class_method :new

    def initialize(instance = nil)
      self.instance = instance
    end

    def respond_to_missing?(*_args, **_kwargs, &_block)
      true
    end

    def method_missing(name, *args, **hargs, &block)
      if /[[:upper:]]/ =~ name[0]
        ATerm::Term.new(*args, constructor: name)
      else
        instance.send(name, *args, **hargs, &block)
      end
    end

    class << self
      def declare(instance = nil, &block)
        node = new(instance).instance_eval(&block)
        node = read(node) if node.is_a?(String)
        node
      end

      def read(text)
        new.instance_eval(text.to_s.delete("\n"))
      end
    end
  end
end
