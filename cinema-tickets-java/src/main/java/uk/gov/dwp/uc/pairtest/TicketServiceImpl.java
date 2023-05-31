package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */
    public static final int MAXIMUM_PURCHASABLE_TICKET = 20;
    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        validateTicketRequests(ticketTypeRequests);

        Boolean hasOneAdultTicket = false;
        Boolean hasInfantOrChildTicket = false;
        int totalTicketsCount = 0;
        int seatsToBeAllocated = 0;
        int totalPurchaseAmount = 0;

        for(TicketTypeRequest ticket : ticketTypeRequests){
            int currentTicketCount = ticket.getNoOfTickets();

            if(ticket.getTicketType() == TicketTypeRequest.Type.CHILD || ticket.getTicketType() == TicketTypeRequest.Type.INFANT){
                hasInfantOrChildTicket = true;
            }
            if(ticket.getTicketType() == TicketTypeRequest.Type.ADULT){
                hasOneAdultTicket = true;
            }
            if(ticket.getTicketType() != TicketTypeRequest.Type.INFANT){
                seatsToBeAllocated += currentTicketCount;
                totalPurchaseAmount += getTicketTypeCost(ticket.getTicketType()) *  currentTicketCount;
            }
            totalTicketsCount += currentTicketCount;

            if(totalTicketsCount > MAXIMUM_PURCHASABLE_TICKET){
                throw new InvalidPurchaseException("Maximum ticket allowed for purchase exceeded");
            }
        }


        if(hasInfantOrChildTicket == true && hasOneAdultTicket == false){
            throw new InvalidPurchaseException("Child and Infant tickets cannot be purchased without purchasing an Adult ticket");
        }

        ticketPaymentService.makePayment(accountId,totalPurchaseAmount);
        seatReservationService.reserveSeat(accountId,seatsToBeAllocated);
    }

    private void validateTicketRequests(TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("Invalid number of tickets requested.");
        }
    }
    private int getTicketTypeCost(TicketTypeRequest.Type type){
        int ticketCost = 0;
        switch (type){
            case INFANT:
                ticketCost = 0;
                break;
            case CHILD:
                ticketCost = 10;
                break;
            case ADULT:
                ticketCost = 20;
                break;
            default:
                break;

        }
        return ticketCost;
    }

}
