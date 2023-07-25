module ATerm
  module PrettyPrinting
    C_HIGHLIGHT = "\033[0;32m".freeze
    C_NONE = "\033[0m".freeze

    def inspect
      "#<ATerm::#{constructor}>"
    end

    def to_s(level: 0, min: false, highlight: [])
      highlight = Array(highlight).filter_map(&:to_s)
      if highlight.include?(constructor.to_s)
        open_bracket = "#{C_HIGHLIGHT}#{constructor}#{C_NONE}("
      else
        open_bracket = "#{constructor}("
      end
      if min
        wrap_s_min(arguments, open_bracket, ')', level: level, highlight: highlight)
      else
        wrap_s(arguments, open_bracket, ')', level: level, highlight: highlight)
      end
    end

    protected

    def wrap_s(list, open_bracket, close_bracket, level: 0, highlight: [])
      if list.all? { |node| leaf?(node) }
        "#{open_bracket}#{list.map do |x|
          if primitive?(x)
            x.inspect
          else
            x.to_s(
                level: level + 1, min: false, highlight: highlight
              ).delete_suffix(',')
          end
        end.join(', ')}#{close_bracket}#{',' if level != 0}"
      else
        string = <<~STRING
          #{open_bracket}
          #{children = list.each_with_index.map do |child, index|
              if child.is_a?(Array)
                child_s = wrap_s(child, '[', ']', level: level + 1, highlight: highlight)
              elsif primitive?(child)
                child_s = "#{child.inspect},"
              else
                child_s = child.to_s(
                    level: level + 1, min: false, highlight: highlight
                  )
              end
              child_s = child_s.strip.sub(/,\s*\z/, '') if index >= list.size - 1
              child_s
            end
            children.join("\n").split("\n").map { |x| x.prepend(' ' * 2) }.join("\n")}
          #{close_bracket},
        STRING
        string = string.strip
        string = string.strip[0..(string.size - 2)] if level == 0
        string
      end
    end

    def wrap_s_min(list, open_bracket, close_bracket, level: 0, highlight: [])
      if list.all? { |node| leaf?(node) }
        "#{open_bracket}#{list.map do |x|
          if primitive?(x)
            x.inspect
          else
            x.to_s(
                level: level + 1, min: false, highlight: highlight
              ).delete_suffix(',')
          end
        end.join(',')}#{close_bracket}#{',' if level != 0}"
      else
        string = open_bracket
        list.each_with_index do |child, index|
          if child.is_a?(Array)
            child_s = wrap_s_min(child, '[', ']', level: level + 1, highlight: highlight)
          elsif primitive?(child)
            child_s = "#{child.inspect},"
          else
            child_s = child.to_s(
                level: level + 1, min: true, highlight: highlight
              )
          end
          child_s = child_s.strip.sub(/,\s*\z/, '') if index >= list.size - 1
          string << child_s
        end
        string << close_bracket
        string << ','
        string = string.strip
        string = string.strip[0..(string.size - 2)] if level == 0
        string
      end
    end
  end
end
