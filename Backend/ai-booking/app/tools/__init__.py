from .base_tool import BaseTool
from .ticket_tools import CreateDraftBookingTool, GetSuggestedSeatsTool
from .reminder_tools import CreateDraftReminderTool
from .user_tools import GetUserTicketsTool

create_draft_booking = CreateDraftBookingTool()
get_suggested_seats = GetSuggestedSeatsTool()
create_draft_reminder = CreateDraftReminderTool()
get_user_tickets = GetUserTicketsTool()

ALL_MOCK_TOOLS = [
    create_draft_booking,
    get_suggested_seats,
    create_draft_reminder,
    get_user_tickets
]
