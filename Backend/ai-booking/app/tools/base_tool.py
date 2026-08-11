class BaseTool:
    def __init__(self, name: str, description: str):
        self.name = name
        self.description = description

    def execute(self, *args, **kwargs) -> str:
        raise NotImplementedError("Each tool must implement the execute method.")
